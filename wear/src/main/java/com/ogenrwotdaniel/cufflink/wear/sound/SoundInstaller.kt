package com.ogenrwotdaniel.cufflink.wear.sound

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.data.WatchStorage

/**
 * Turns an audio file into a watch ringtone, notification sound or alarm.
 *
 * This is the problem Cufflink was started for: Galaxy Watch Ultra settings only offer
 * the sounds already in the system picker, and there is no on-watch way to put a
 * file there. Two steps, and either is useful alone:
 *
 * 1. **Stage** — copy the file into MediaStore under `Ringtones/`, `Notifications/`
 *    or `Alarms/` with the matching IS_* flag. That alone makes it appear in the
 *    watch's own Settings → Sounds picker. Needs no permission.
 * 2. **Apply** — set it as the default. Needs WRITE_SETTINGS, which on many
 *    watches has no settings screen and must be granted once over adb
 *    (`tools/grant-watch-powers.sh`). Without it, step 1 still worked.
 */
class SoundInstaller(context: Context) {
    private val context = context.applicationContext
    private val storage = WatchStorage(this.context)

    sealed interface Outcome {
        val uri: Uri
        /** Set as the default — done. */
        data class Applied(override val uri: Uri) : Outcome
        /** In the system picker, but Cufflink may not switch it on by itself. */
        data class Staged(override val uri: Uri) : Outcome
    }

    fun canApply(): Boolean = Settings.System.canWrite(context)

    fun install(entry: Entry, role: SoundRole): Outcome {
        require(role != SoundRole.NONE) { "No sound role" }
        val uri = stage(entry, role)
        if (!canApply()) return Outcome.Staged(uri)
        RingtoneManager.setActualDefaultRingtoneUri(context, typeOf(role), uri)
        return Outcome.Applied(uri)
    }

    /** Title of the sound currently set for [role], or null if none / unreadable. */
    fun currentTitle(role: SoundRole): String? = runCatching {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, typeOf(role)) ?: return null
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull()

    fun currentUri(role: SoundRole): Uri? =
        runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, typeOf(role)) }.getOrNull()

    private fun stage(entry: Entry, role: SoundRole): Uri {
        val folder = "${folderOf(role)}/"
        existing(entry.name, folder)?.let { return it }
        val flags = ContentValues().apply {
            put(MediaStore.Audio.AudioColumns.TITLE, entry.name.substringBeforeLast('.'))
            put(MediaStore.Audio.AudioColumns.IS_RINGTONE, role == SoundRole.RINGTONE)
            put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, role == SoundRole.NOTIFICATION)
            put(MediaStore.Audio.AudioColumns.IS_ALARM, role == SoundRole.ALARM)
            put(MediaStore.Audio.AudioColumns.IS_MUSIC, false)
        }
        return storage.insertCopy(entry, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, folder, flags)
    }

    /** Re-installing the same sound reuses its row instead of piling up "song (2).mp3". */
    private fun existing(name: String, relativePath: String): Uri? {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        return context.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(name, relativePath),
            null,
        )?.use { c -> if (c.moveToFirst()) android.content.ContentUris.withAppendedId(collection, c.getLong(0)) else null }
    }

    companion object {
        fun typeOf(role: SoundRole): Int = when (role) {
            SoundRole.RINGTONE -> RingtoneManager.TYPE_RINGTONE
            SoundRole.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
            SoundRole.ALARM -> RingtoneManager.TYPE_ALARM
            SoundRole.NONE -> error("No sound role")
        }

        fun folderOf(role: SoundRole): String = when (role) {
            SoundRole.RINGTONE -> Environment.DIRECTORY_RINGTONES
            SoundRole.NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
            SoundRole.ALARM -> Environment.DIRECTORY_ALARMS
            SoundRole.NONE -> error("No sound role")
        }

        fun label(role: SoundRole): String = when (role) {
            SoundRole.RINGTONE -> "Ringtone"
            SoundRole.NOTIFICATION -> "Notification"
            SoundRole.ALARM -> "Alarm"
            SoundRole.NONE -> "File"
        }

        /** Grant command for watches with no WRITE_SETTINGS screen. */
        const val ADB_WRITE_SETTINGS =
            "adb shell appops set com.ogenrwotdaniel.cufflink WRITE_SETTINGS allow"
        const val ADB_ALL_FILES =
            "adb shell appops set com.ogenrwotdaniel.cufflink MANAGE_EXTERNAL_STORAGE allow"
    }
}
