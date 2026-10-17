package com.ogenrwotdaniel.cufflink.wear.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.MediaStore
import com.ogenrwotdaniel.cufflink.shared.FileKind
import java.io.File
import java.io.IOException
import java.io.InputStream

/** The three MediaStore shelves the watch can read with granular media permissions. */
enum class MediaShelf(val title: String, val glyph: String, val collection: Uri, val permission: String) {
    IMAGES("Pictures", "◐", MediaStore.Images.Media.EXTERNAL_CONTENT_URI, android.Manifest.permission.READ_MEDIA_IMAGES),
    SOUNDS("Sounds", "♪", MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, android.Manifest.permission.READ_MEDIA_AUDIO),
    VIDEOS("Videos", "▶", MediaStore.Video.Media.EXTERNAL_CONTENT_URI, android.Manifest.permission.READ_MEDIA_VIDEO),
}

/** Free and total bytes of the watch's internal storage. */
data class StorageStats(val freeBytes: Long, val totalBytes: Long) {
    val usedFraction: Float
        get() = if (totalBytes <= 0) 0f else ((totalBytes - freeBytes).toFloat() / totalBytes).coerceIn(0f, 1f)
}

/**
 * Every read and write Cufflink makes to the watch's storage.
 *
 * Two worlds, deliberately: the **Pocket** (app-private, always readable, where
 * beams land) and the **shared storage** (MediaStore with READ_MEDIA_*, or the
 * whole tree when all-files access has been granted over adb). Wear OS has no
 * system screen to grant all-files access on most watches, so the app must be
 * fully useful without it.
 */
class WatchStorage(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver

    val pocketDir: File = File(this.context.filesDir, "pocket").apply { mkdirs() }

    /** Root of shared storage — only listable with all-files access. */
    val sharedRoot: File get() = Environment.getExternalStorageDirectory()

    fun hasAllFilesAccess(): Boolean = Environment.isExternalStorageManager()

    fun stats(): StorageStats {
        val fs = StatFs(Environment.getDataDirectory().path)
        return StorageStats(fs.availableBytes, fs.totalBytes)
    }

    fun pocket(): List<Entry> =
        (pocketDir.listFiles() ?: emptyArray())
            .filter { it.isFile && !it.name.startsWith(".") }
            .sortedByDescending { it.lastModified() }
            .map { entryFor(it, inPocket = true) }

    fun pocketFile(name: String): Entry? =
        File(pocketDir, name).takeIf { it.isFile && it.parentFile == pocketDir }?.let { entryFor(it, true) }

    /** Folders first, then files, each alphabetically. Hidden entries stay hidden. */
    fun listFolder(dir: File): List<Entry> =
        (dir.listFiles() ?: emptyArray())
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            .map { entryFor(it, inPocket = false) }

    fun mediaShelf(shelf: MediaShelf): List<Entry> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val out = mutableListOf<Entry>()
        resolver.query(shelf.collection, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")
            ?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(4) ?: ""
                    out += Entry(
                        name = name,
                        kind = FileKind.of(name, mime),
                        sizeBytes = c.getLong(2),
                        modifiedMillis = c.getLong(3) * 1000,
                        mimeType = mime,
                        source = Source.Media(ContentUris.withAppendedId(shelf.collection, c.getLong(0))),
                    )
                }
            }
        return out
    }

    fun openInput(entry: Entry): InputStream = when (val s = entry.source) {
        is Source.Disk -> s.file.inputStream()
        is Source.Media -> resolver.openInputStream(s.uri) ?: throw IOException("Cannot open ${entry.name}")
    }

    fun openFd(entry: Entry): ParcelFileDescriptor = when (val s = entry.source) {
        is Source.Disk -> ParcelFileDescriptor.open(s.file, ParcelFileDescriptor.MODE_READ_ONLY)
        is Source.Media -> resolver.openFileDescriptor(s.uri, "r") ?: throw IOException("Cannot open ${entry.name}")
    }

    /** Delete. A MediaStore item owned by another app refuses — reported as false, not thrown. */
    fun delete(entry: Entry): Boolean = when (val s = entry.source) {
        is Source.Disk -> if (s.file.isDirectory) false else s.file.delete()
        is Source.Media -> runCatching { resolver.delete(s.uri, null, null) > 0 }.getOrDefault(false)
    }

    /**
     * Copy a Pocket file out into shared storage, where other watch apps (gallery,
     * music player) can find it. Goes in a "Cufflink" folder under the type's
     * conventional directory. Needs no permission: an app may always add to MediaStore.
     */
    fun saveToShared(entry: Entry): Uri {
        val (collection, dir) = when (entry.kind) {
            FileKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_PICTURES
            FileKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MUSIC
            FileKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MOVIES
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_DOWNLOADS
        }
        return insertCopy(entry, collection, "$dir/Cufflink/", ContentValues())
    }

    /** Insert [entry]'s bytes as a new pending MediaStore row, then publish it. */
    fun insertCopy(entry: Entry, collection: Uri, relativePath: String, extra: ContentValues): Uri {
        val values = ContentValues(extra).apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entry.name)
            put(MediaStore.MediaColumns.MIME_TYPE, entry.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore refused ${entry.name}")
        try {
            resolver.openOutputStream(uri)?.use { out -> openInput(entry).use { it.copyTo(out) } }
                ?: throw IOException("Cannot write ${entry.name}")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    private fun entryFor(file: File, inPocket: Boolean): Entry {
        val kind = if (file.isDirectory) FileKind.FOLDER else FileKind.of(file.name)
        return Entry(
            name = file.name,
            kind = kind,
            sizeBytes = if (file.isDirectory) (file.list()?.size ?: 0).toLong() else file.length(),
            modifiedMillis = file.lastModified(),
            mimeType = if (file.isDirectory) "" else FileKind.guessMime(file.name),
            source = Source.Disk(file),
            inPocket = inPocket,
        )
    }
}
