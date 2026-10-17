package com.ogenrwotdaniel.cufflink.beam

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ogenrwotdaniel.cufflink.shared.BeamHeader
import com.ogenrwotdaniel.cufflink.shared.BeamTransport
import com.ogenrwotdaniel.cufflink.shared.FileKind
import com.ogenrwotdaniel.cufflink.shared.FileNames
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** One file on its way to the watch. */
data class Transfer(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val role: SoundRole,
    val sentBytes: Long = 0,
    val state: State = State.WAITING,
    val message: String? = null,
) {
    enum class State { WAITING, SENDING, DONE, FAILED }
}

/** One file the watch sent back, saved in Downloads/Cufflink. */
data class Arrival(val name: String, val uri: Uri, val sizeBytes: Long)

/**
 * The phone's outbox. Lives at process scope, not in a ViewModel, so a beam
 * started from the share sheet keeps going after that screen closes.
 *
 * Beams go ONE AT A TIME: two channels in parallel over Bluetooth are not
 * faster, and a queue makes the progress list read top to bottom.
 */
object BeamQueue {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val oneAtATime = Mutex()
    private val ids = AtomicLong()

    private val _transfers = MutableStateFlow<List<Transfer>>(emptyList())
    val transfers: StateFlow<List<Transfer>> = _transfers

    private val _arrivals = MutableStateFlow<List<Arrival>>(emptyList())
    val arrivals: StateFlow<List<Arrival>> = _arrivals

    fun arrived(arrival: Arrival) = _arrivals.update { listOf(arrival) + it }

    fun clearFinished() = _transfers.update { list -> list.filter { it.state == Transfer.State.WAITING || it.state == Transfer.State.SENDING } }

    fun beam(context: Context, uris: List<Uri>, role: SoundRole = SoundRole.NONE) {
        val app = context.applicationContext
        val transport = BeamTransport(app)
        uris.forEach { uri ->
            val meta = describe(app, uri)
            val transfer = Transfer(ids.incrementAndGet(), meta.name, meta.size, role)
            _transfers.update { it + transfer }
            scope.launch {
                oneAtATime.withLock { send(app, transport, transfer.id, uri, meta, role) }
            }
        }
    }

    private suspend fun send(context: Context, transport: BeamTransport, id: Long, uri: Uri, meta: Meta, role: SoundRole) {
        edit(id) { it.copy(state = Transfer.State.SENDING, message = "Finding your watch…") }
        var staged: File? = null
        try {
            val watch = transport.peers().sortedByDescending { it.isNearby }.firstOrNull()
                ?: throw IllegalStateException("No watch with Cufflink in reach")
            // The header promises an exact size. A provider that cannot say how big a
            // file is gets copied to cache first, so the promise can be kept.
            var size = meta.size
            val open: () -> java.io.InputStream
            if (size < 0) {
                val tmp = File(context.cacheDir, "beam-$id").also { staged = it }
                context.contentResolver.openInputStream(uri)!!.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                size = tmp.length()
                open = { tmp.inputStream() }
            } else {
                open = { context.contentResolver.openInputStream(uri) ?: error("Cannot read ${meta.name}") }
            }
            edit(id) { it.copy(sizeBytes = size, message = "To ${watch.displayName}") }
            transport.send(watch.id, BeamHeader(meta.name, meta.mime, size, role), open) { sent ->
                edit(id) { it.copy(sentBytes = sent) }
            }
            edit(id) { it.copy(state = Transfer.State.DONE, sentBytes = size, message = "On ${watch.displayName}") }
        } catch (t: Throwable) {
            edit(id) { it.copy(state = Transfer.State.FAILED, message = t.message ?: "Failed") }
        } finally {
            staged?.delete()
        }
    }

    private fun edit(id: Long, change: (Transfer) -> Transfer) =
        _transfers.update { list -> list.map { if (it.id == id) change(it) else it } }

    private data class Meta(val name: String, val size: Long, val mime: String)

    private fun describe(context: Context, uri: Uri): Meta {
        var name: String? = null
        var size = -1L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        name = c.getString(0)
                        if (!c.isNull(1)) size = c.getLong(1)
                    }
                }
        }
        val safe = FileNames.sanitize(name ?: uri.lastPathSegment ?: "beam")
        val mime = context.contentResolver.getType(uri) ?: FileKind.guessMime(safe)
        return Meta(safe, size, mime)
    }
}
