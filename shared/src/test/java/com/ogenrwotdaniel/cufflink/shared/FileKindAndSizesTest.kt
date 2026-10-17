package com.ogenrwotdaniel.cufflink.shared

import org.junit.Assert.assertEquals
import org.junit.Test

class FileKindAndSizesTest {
    @Test fun mimeWinsOverExtension() {
        assertEquals(FileKind.AUDIO, FileKind.of("download.bin", "audio/mpeg"))
        assertEquals(FileKind.PDF, FileKind.of("x", "application/pdf"))
    }

    @Test fun extensionCoversVagueMime() {
        assertEquals(FileKind.AUDIO, FileKind.of("chime.ogg", "application/octet-stream"))
        assertEquals(FileKind.IMAGE, FileKind.of("photo.HEIC"))
        assertEquals(FileKind.OTHER, FileKind.of("mystery"))
    }

    @Test fun onlyAudioCanBeASound() {
        assertEquals(listOf(FileKind.AUDIO), FileKind.entries.filter { it.canBeSound })
    }

    @Test fun sizesStayShort() {
        assertEquals("640 B", Sizes.format(640))
        assertEquals("1.0 KB", Sizes.format(1024))
        assertEquals("3.2 MB", Sizes.format((3.2 * 1024 * 1024).toLong()))
        assertEquals("32 GB", Sizes.format(32L * 1024 * 1024 * 1024))
        assertEquals("?", Sizes.format(-1))
    }

    @Test fun fractionIsSafe() {
        assertEquals(0f, Sizes.fraction(5, 0))
        assertEquals(1f, Sizes.fraction(15, 10))
        assertEquals(0.5f, Sizes.fraction(5, 10))
    }
}
