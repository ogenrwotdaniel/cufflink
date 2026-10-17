package com.ogenrwotdaniel.cufflink.shared

/**
 * Pure rules about file names. A name arriving over the wire is untrusted: it
 * decides where bytes land on disk, so it must never be able to climb out of the
 * folder it is written into.
 */
object FileNames {
    private const val MAX_LENGTH = 120

    /**
     * Reduce [raw] to a safe single path segment: no separators, no `..`, no control
     * characters, not hidden, bounded length with the extension kept.
     */
    fun sanitize(raw: String): String {
        val lastSegment = raw.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = lastSegment
            .filter { it >= ' ' && it !in "<>:\"|?*" }
            .trim()
            .trimStart('.')
            .trim()
        if (cleaned.isEmpty()) return "beam"
        if (cleaned.length <= MAX_LENGTH) return cleaned
        val ext = extensionOf(cleaned)
        val keep = MAX_LENGTH - (if (ext.isEmpty()) 0 else ext.length + 1)
        val base = cleaned.substring(0, keep).trimEnd()
        return if (ext.isEmpty()) base else "$base.$ext"
    }

    /** Lower-case extension without the dot, or "" when there is none. */
    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return ""
        return name.substring(dot + 1).lowercase()
    }

    /**
     * First name not in [taken]: `song.mp3`, then `song (2).mp3`, `song (3).mp3`…
     * A second beam of the same file must not silently overwrite the first.
     */
    fun unique(name: String, taken: Set<String>): String {
        if (name !in taken) return name
        val ext = extensionOf(name)
        val base = if (ext.isEmpty()) name else name.dropLast(ext.length + 1)
        var n = 2
        while (true) {
            val candidate = if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext"
            if (candidate !in taken) return candidate
            n++
        }
    }
}
