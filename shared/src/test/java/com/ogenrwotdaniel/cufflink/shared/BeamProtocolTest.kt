package com.ogenrwotdaniel.cufflink.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class BeamProtocolTest {
    private fun roundTrip(header: BeamHeader): BeamHeader {
        val out = ByteArrayOutputStream()
        BeamProtocol.writeHeader(out, header)
        out.write(byteArrayOf(1, 2, 3)) // payload follows the header
        val input = ByteArrayInputStream(out.toByteArray())
        val read = BeamProtocol.readHeader(input)
        assertEquals("payload starts right after the header", 1, input.read())
        return read
    }

    @Test fun headerRoundTrips() {
        val h = BeamHeader("Morning Bell.ogg", "audio/ogg", 48_213, SoundRole.ALARM)
        assertEquals(h, roundTrip(h))
    }

    @Test fun nameIsSanitisedOnTheReceivingEnd() {
        val h = BeamHeader("../../shared_prefs/evil.xml", "text/xml", 10)
        assertEquals("evil.xml", roundTrip(h).name)
    }

    @Test fun blankMimeBecomesOctetStream() {
        assertEquals("application/octet-stream", roundTrip(BeamHeader("a.bin", "", 1)).mimeType)
    }

    @Test fun unknownRoleFallsBackToNone() {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            writeInt(BeamProtocol.MAGIC); writeInt(BeamProtocol.VERSION)
            writeUTF("a.mp3"); writeUTF("audio/mpeg"); writeLong(5); writeUTF("DOORBELL")
        }
        assertEquals(SoundRole.NONE, BeamProtocol.readHeader(ByteArrayInputStream(out.toByteArray())).role)
    }

    @Test fun wrongMagicIsRefused() {
        val bytes = ByteArrayOutputStream().also { DataOutputStream(it).writeInt(0x12345678) }.toByteArray()
        assertThrows(IOException::class.java) { BeamProtocol.readHeader(ByteArrayInputStream(bytes)) }
    }

    @Test fun oversizedBeamIsRefused() {
        val out = ByteArrayOutputStream()
        BeamProtocol.writeHeader(out, BeamHeader("huge.mkv", "video/x-matroska", BeamProtocol.MAX_BYTES + 1))
        assertThrows(IOException::class.java) { BeamProtocol.readHeader(ByteArrayInputStream(out.toByteArray())) }
    }
}
