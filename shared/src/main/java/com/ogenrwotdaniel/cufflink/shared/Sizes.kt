package com.ogenrwotdaniel.cufflink.shared

import java.util.Locale
import kotlin.math.roundToInt

/** Human sizes sized for a watch face: "640 B", "3.2 MB", "12 GB" — never more than ~6 characters. */
object Sizes {
    fun format(bytes: Long): String {
        if (bytes < 0) return "?"
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return if (value < 10) String.format(Locale.ROOT, "%.1f %s", value, units[unit])
        else "${value.roundToInt()} ${units[unit]}"
    }

    /** 0..1 progress, safe for an unknown (0) total. */
    fun fraction(done: Long, total: Long): Float =
        if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
}
