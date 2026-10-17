package com.ogenrwotdaniel.cufflink.wear

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ogenrwotdaniel.cufflink.shared.BeamHeader
import com.ogenrwotdaniel.cufflink.shared.BeamTransport
import com.ogenrwotdaniel.cufflink.shared.Sizes
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import com.ogenrwotdaniel.cufflink.wear.beam.PocketEvents
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.data.MediaShelf
import com.ogenrwotdaniel.cufflink.wear.data.StorageStats
import com.ogenrwotdaniel.cufflink.wear.data.WatchStorage
import com.ogenrwotdaniel.cufflink.wear.sound.SoundInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Holds what the watch screens show. One ViewModel for the whole app: the
 * screens are small and share a lot (the selected file travels from a list to
 * the viewer to the actions screen).
 */
class CufflinkViewModel(app: Application) : AndroidViewModel(app) {
    val storage = WatchStorage(app)
    val sounds = SoundInstaller(app)
    private val transport = BeamTransport(app)

    var stats by mutableStateOf(StorageStats(0, 0)); private set
    var pocket by mutableStateOf<List<Entry>>(emptyList()); private set
    var listing by mutableStateOf<List<Entry>>(emptyList()); private set
    var loading by mutableStateOf(false); private set

    /** The file a viewer or the actions screen is looking at. */
    var selected by mutableStateOf<Entry?>(null)

    /** One-line status shown on the actions screen ("Sent to phone ✓"). */
    var status by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch { PocketEvents.arrivals.collect { refreshHome() } }
    }

    fun refreshHome() = viewModelScope.launch {
        val (s, p) = withContext(Dispatchers.IO) { storage.stats() to storage.pocket() }
        stats = s
        pocket = p
    }

    fun loadShelf(shelf: MediaShelf) = load { storage.mediaShelf(shelf) }

    fun loadFolder(dir: File) = load { storage.listFolder(dir) }

    /** Every sound the watch can see: Pocket audio first, then the media library. */
    fun loadSounds(includeLibrary: Boolean) = load {
        val pocketAudio = storage.pocket().filter { it.kind.canBeSound }
        val library = if (includeLibrary) runCatching { storage.mediaShelf(MediaShelf.SOUNDS) }.getOrDefault(emptyList()) else emptyList()
        pocketAudio + library
    }

    private fun load(block: () -> List<Entry>) = viewModelScope.launch {
        loading = true
        listing = emptyList()
        listing = withContext(Dispatchers.IO) { runCatching(block).getOrDefault(emptyList()) }
        loading = false
    }

    fun openPocketFile(name: String): Boolean {
        val entry = storage.pocketFile(name) ?: return false
        selected = entry
        return true
    }

    fun clearStatus() {
        status = null
    }

    fun installSound(entry: Entry, role: SoundRole) = viewModelScope.launch {
        status = "Installing…"
        status = withContext(Dispatchers.IO) {
            runCatching { sounds.install(entry, role) }.fold(
                onSuccess = {
                    when (it) {
                        is SoundInstaller.Outcome.Applied -> "✓ Now your ${SoundInstaller.label(role).lowercase()} sound"
                        is SoundInstaller.Outcome.Staged -> "Added to Settings › Sounds. Grant \"Change sounds\" in Powers to switch it on here."
                    }
                },
                onFailure = { "Couldn't: ${it.message}" },
            )
        }
    }

    fun saveToShared(entry: Entry) = viewModelScope.launch {
        status = "Saving…"
        status = withContext(Dispatchers.IO) {
            runCatching { storage.saveToShared(entry) }.fold(
                onSuccess = { "✓ Saved to watch storage" },
                onFailure = { "Couldn't save: ${it.message}" },
            )
        }
    }

    /** Returns true if gone; the caller pops back to the list. */
    suspend fun delete(entry: Entry): Boolean {
        val gone = withContext(Dispatchers.IO) { storage.delete(entry) }
        if (gone) {
            selected = null
            listing = listing - entry
            refreshHome()
        } else {
            status = "This file belongs to another app and can't be deleted from here."
        }
        return gone
    }

    /** Beam a watch file back to the phone (it lands in the phone's Downloads/Cufflink). */
    fun sendToPhone(entry: Entry) = viewModelScope.launch {
        status = "Looking for your phone…"
        val phone = runCatching { transport.peers().firstOrNull() }.getOrNull()
        if (phone == null) {
            status = "No phone with Cufflink in reach"
            return@launch
        }
        val header = BeamHeader(entry.name, entry.mimeType.ifBlank { "application/octet-stream" }, entry.sizeBytes)
        status = runCatching {
            transport.send(phone.id, header, { storage.openInput(entry) }) { sent ->
                status = "Sending ${(Sizes.fraction(sent, entry.sizeBytes) * 100).toInt()}%"
            }
        }.fold(
            onSuccess = { "✓ Sent to ${phone.displayName}" },
            onFailure = { "Send failed: ${it.message}" },
        )
    }
}
