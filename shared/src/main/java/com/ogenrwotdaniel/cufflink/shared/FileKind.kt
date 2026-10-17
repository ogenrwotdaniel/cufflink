package com.ogenrwotdaniel.cufflink.shared

/**
 * How a file is shown: which viewer opens it and which glyph marks it in a list.
 * Decided from the MIME type first and the extension second, because both
 * MediaStore and the phone's document providers are often vague about MIME.
 */
enum class FileKind(val glyph: String, val label: String) {
    IMAGE("◐", "Image"),
    AUDIO("♪", "Sound"),
    VIDEO("▶", "Video"),
    TEXT("≡", "Text"),
    PDF("❏", "PDF"),
    ARCHIVE("▣", "Archive"),
    FOLDER("▸", "Folder"),
    OTHER("◇", "File");

    /** Whether the watch can set this as a ringtone / notification / alarm sound. */
    val canBeSound: Boolean get() = this == AUDIO

    companion object {
        private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "avif")
        private val AUDIO_EXT = setOf("mp3", "ogg", "oga", "opus", "m4a", "aac", "wav", "flac", "amr", "mid", "midi", "3gp")
        private val VIDEO_EXT = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gpp")
        private val TEXT_EXT = setOf("txt", "md", "csv", "json", "xml", "log", "srt", "lrc", "ini", "yaml", "yml", "kt", "java", "py", "html", "htm", "css", "js")
        private val ARCHIVE_EXT = setOf("zip", "7z", "rar", "tar", "gz", "apk")

        fun of(name: String, mimeType: String? = null): FileKind {
            val mime = mimeType?.lowercase().orEmpty()
            when {
                mime == "vnd.android.document/directory" -> return FOLDER
                mime == "application/pdf" -> return PDF
                mime.startsWith("image/") -> return IMAGE
                mime.startsWith("audio/") || mime == "application/ogg" -> return AUDIO
                mime.startsWith("video/") -> return VIDEO
                mime.startsWith("text/") -> return TEXT
            }
            return when (FileNames.extensionOf(name)) {
                in IMAGE_EXT -> IMAGE
                in AUDIO_EXT -> AUDIO
                in VIDEO_EXT -> VIDEO
                in TEXT_EXT -> TEXT
                in ARCHIVE_EXT -> ARCHIVE
                "pdf" -> PDF
                else -> OTHER
            }
        }

        /** A MIME type good enough for MediaStore and intents when the source gave none. */
        fun guessMime(name: String): String = when (FileNames.extensionOf(name)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp3" -> "audio/mpeg"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/opus"
            "m4a", "aac" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "pdf" -> "application/pdf"
            "txt", "log", "md" -> "text/plain"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
