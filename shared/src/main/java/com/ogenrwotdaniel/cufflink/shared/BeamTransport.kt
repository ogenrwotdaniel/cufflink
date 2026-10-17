package com.ogenrwotdaniel.cufflink.shared

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Moves files between phone and watch over a Wearable [ChannelClient] channel.
 *
 * Used identically from both ends — the phone beams to the watch and the watch
 * beams back — so it lives here rather than in either app.
 *
 * A channel, not a DataItem/Asset: an Asset is capped and buffered whole in the
 * Data Layer, while a channel streams, reports progress byte by byte and holds
 * nothing once the transfer is done.
 */
class BeamTransport(context: Context) {
    private val appContext = context.applicationContext
    private val channels = Wearable.getChannelClient(appContext)
    private val capabilities = Wearable.getCapabilityClient(appContext)
    private val nodes = Wearable.getNodeClient(appContext)

    /** Every device currently linked over Bluetooth / Wi-Fi, whether or not Cufflink is on it. */
    suspend fun connectedNodes(): List<Node> = nodes.connectedNodes.await()

    /** Linked devices that have Cufflink installed and can accept a beam. */
    suspend fun peers(): List<Node> =
        capabilities.getCapability(BeamProtocol.CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await().nodes.toList()

    /**
     * Stream one file to [nodeId]. [open] is called once, on an IO thread; the caller
     * owns nothing afterwards. [onProgress] receives bytes sent so far.
     */
    suspend fun send(
        nodeId: String,
        header: BeamHeader,
        open: () -> InputStream,
        onProgress: (Long) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val channel = channels.openChannel(nodeId, BeamProtocol.CHANNEL_PATH).await()
        try {
            channels.getOutputStream(channel).await().use { out ->
                BeamProtocol.writeHeader(out, header)
                open().use { input -> copy(input, out, onProgress) }
                out.flush()
            }
        } finally {
            runCatching { channels.close(channel).await() }
        }
    }

    /**
     * Read one beam from [channel] into [folder]. Written to a hidden `.part` file and
     * renamed only once every promised byte has arrived, so a dropped connection
     * never leaves a truncated song sitting in the Pocket looking complete.
     */
    suspend fun receive(
        channel: ChannelClient.Channel,
        folder: File,
        onProgress: (BeamHeader, Long) -> Unit = { _, _ -> },
    ): ReceivedBeam = withContext(Dispatchers.IO) {
        folder.mkdirs()
        try {
            channels.getInputStream(channel).await().use { input ->
                val header = BeamProtocol.readHeader(input)
                if (folder.usableSpace < header.sizeBytes + RESERVE_BYTES) {
                    throw IOException("Not enough space for ${header.name} (${Sizes.format(header.sizeBytes)})")
                }
                val part = File(folder, ".${System.nanoTime()}.part")
                try {
                    val written = part.outputStream().use { out ->
                        copy(input, out) { onProgress(header, it) }
                    }
                    if (written != header.sizeBytes) {
                        throw IOException("Beam cut short: $written of ${header.sizeBytes} bytes")
                    }
                    val taken = folder.list()?.toSet().orEmpty()
                    val target = File(folder, FileNames.unique(header.name, taken))
                    if (!part.renameTo(target)) throw IOException("Could not file ${header.name}")
                    ReceivedBeam(target, header)
                } finally {
                    part.delete()
                }
            }
        } finally {
            runCatching { channels.close(channel).await() }
        }
    }

    private fun copy(input: InputStream, out: java.io.OutputStream, onProgress: (Long) -> Unit): Long {
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0L
        var lastReport = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            total += n
            if (total - lastReport >= REPORT_EVERY_BYTES) {
                lastReport = total
                onProgress(total)
            }
        }
        onProgress(total)
        return total
    }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
        const val REPORT_EVERY_BYTES = 128 * 1024L
        /** Never fill the watch to the last byte; the OS needs room to breathe. */
        const val RESERVE_BYTES = 64L * 1024 * 1024
    }
}

/** A beam that arrived intact. */
data class ReceivedBeam(val file: File, val header: BeamHeader)
