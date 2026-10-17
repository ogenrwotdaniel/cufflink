package com.ogenrwotdaniel.cufflink.shared

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The wire contract for a "beam": one file, sent over a Wearable ChannelClient
 * channel, in either direction.
 *
 * A channel is a raw byte pipe — it carries no name, size or type. So every beam
 * starts with a small [BeamHeader] written by [BeamProtocol.writeHeader] and read
 * back by [BeamProtocol.readHeader]; the file's bytes follow immediately.
 *
 * The header is length-prefixed fields behind a magic number and a version, not
 * JSON: there is nothing to parse leniently, and a stream that does not start with
 * the magic is refused outright rather than written to disk as garbage.
 */
object BeamProtocol {
    /** Channel path used for every beam, both phone→watch and watch→phone. */
    const val CHANNEL_PATH = "/cufflink/beam"

    /** Capability both apps advertise (res/values/wear.xml) so each can find the other. */
    const val CAPABILITY = "cufflink_beam"

    /** "CUFF" in ASCII. */
    const val MAGIC = 0x43554646
    const val VERSION = 1

    /** Refuse anything larger. The watch has room, but a beam this big is a mistake. */
    const val MAX_BYTES: Long = 512L * 1024 * 1024

    @Throws(IOException::class)
    fun writeHeader(out: OutputStream, header: BeamHeader) {
        val data = DataOutputStream(out)
        data.writeInt(MAGIC)
        data.writeInt(VERSION)
        data.writeUTF(header.name)
        data.writeUTF(header.mimeType)
        data.writeLong(header.sizeBytes)
        data.writeUTF(header.role.name)
        data.flush()
    }

    @Throws(IOException::class)
    fun readHeader(input: InputStream): BeamHeader {
        val data = DataInputStream(input)
        if (data.readInt() != MAGIC) throw IOException("Not a Cufflink beam")
        val version = data.readInt()
        if (version != VERSION) throw IOException("Unsupported beam version $version")
        val name = FileNames.sanitize(data.readUTF())
        val mime = data.readUTF().ifBlank { "application/octet-stream" }
        val size = data.readLong()
        if (size < 0 || size > MAX_BYTES) throw IOException("Refusing a beam of $size bytes")
        val role = SoundRole.parse(data.readUTF())
        return BeamHeader(name, mime, size, role)
    }
}

/**
 * What travels in front of a beamed file.
 *
 * @property role what the sender wants done with it on arrival. [SoundRole.NONE]
 *   just files it in the Pocket; anything else asks the watch to offer it as that sound.
 */
data class BeamHeader(
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val role: SoundRole = SoundRole.NONE,
)

/** Which system sound slot a file should fill. Parsed leniently: unknown → [NONE]. */
enum class SoundRole {
    NONE, RINGTONE, NOTIFICATION, ALARM;

    companion object {
        fun parse(value: String?): SoundRole =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}
