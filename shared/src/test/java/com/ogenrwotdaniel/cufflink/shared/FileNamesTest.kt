package com.ogenrwotdaniel.cufflink.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {
    @Test fun stripsDirectoriesAndTraversal() {
        assertEquals("passwd", FileNames.sanitize("/etc/passwd"))
        assertEquals("x.txt", FileNames.sanitize("..\\..\\x.txt"))
        assertEquals("beam", FileNames.sanitize(".."))
    }

    @Test fun refusesHiddenAndControlCharacters() {
        assertEquals("bashrc", FileNames.sanitize(".bashrc"))
        assertEquals("ab.mp3", FileNames.sanitize("a\u0000b.mp3"))
        assertEquals("beam", FileNames.sanitize("   "))
    }

    @Test fun longNamesKeepTheirExtension() {
        val long = "a".repeat(300) + ".flac"
        val safe = FileNames.sanitize(long)
        assertTrue(safe.length <= 120)
        assertTrue(safe.endsWith(".flac"))
    }

    @Test fun extensionRules() {
        assertEquals("mp3", FileNames.extensionOf("Song.MP3"))
        assertEquals("", FileNames.extensionOf("README"))
        assertEquals("", FileNames.extensionOf(".hidden"))
        assertEquals("", FileNames.extensionOf("trailing."))
    }

    @Test fun uniqueCountsUpward() {
        val taken = setOf("song.mp3", "song (2).mp3")
        assertEquals("song (3).mp3", FileNames.unique("song.mp3", taken))
        assertEquals("notes (2)", FileNames.unique("notes", setOf("notes")))
        assertEquals("new.txt", FileNames.unique("new.txt", taken))
    }
}
