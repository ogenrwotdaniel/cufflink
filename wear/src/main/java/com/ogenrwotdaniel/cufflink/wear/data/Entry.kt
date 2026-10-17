package com.ogenrwotdaniel.cufflink.wear.data

import android.net.Uri
import com.ogenrwotdaniel.cufflink.shared.FileKind
import java.io.File

/** Where an [Entry]'s bytes actually live. */
sealed interface Source {
    /** A plain file — the Pocket, or anywhere once all-files access is granted. */
    data class Disk(val file: File) : Source

    /** A MediaStore row — what the watch can see with only READ_MEDIA_* granted. */
    data class Media(val uri: Uri) : Source
}

/**
 * One thing in a list on the watch: a file, a folder or a media item. Every
 * screen and viewer works in terms of this, so none of them care which of the
 * two storage worlds a file came from.
 */
data class Entry(
    val name: String,
    val kind: FileKind,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val mimeType: String,
    val source: Source,
    /** True for files sitting in the Pocket (beamed in, private to Cufflink). */
    val inPocket: Boolean = false,
) {
    val isFolder: Boolean get() = kind == FileKind.FOLDER

    /** Human location for the details screen. */
    val location: String
        get() = when (source) {
            is Source.Disk -> if (inPocket) "Pocket" else source.file.parent.orEmpty()
            is Source.Media -> "Media library"
        }
}
