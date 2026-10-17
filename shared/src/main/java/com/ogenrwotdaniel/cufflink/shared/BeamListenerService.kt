package com.ogenrwotdaniel.cufflink.shared

import android.util.Log
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Accepts incoming beams. Each app subclasses it to say where files land and what
 * happens after; the reading itself is [BeamTransport.receive].
 *
 * The listener callback runs on the service's own worker thread, so blocking it
 * for the duration of one transfer is allowed — and is what keeps the service
 * alive until the file is safely on disk.
 */
abstract class BeamListenerService : WearableListenerService() {

    /** Folder the raw beam is written into. */
    abstract fun inboxFolder(): File

    /** Called once the file is complete on disk. Runs on the worker thread. */
    abstract suspend fun onBeamArrived(beam: ReceivedBeam)

    /** Called when a beam fails. Runs on the worker thread. */
    open fun onBeamFailed(error: Throwable) {}

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        if (channel.path != BeamProtocol.CHANNEL_PATH) return
        runBlocking {
            try {
                val beam = BeamTransport(this@BeamListenerService).receive(channel, inboxFolder())
                onBeamArrived(beam)
            } catch (t: Throwable) {
                Log.w(TAG, "Beam failed", t)
                onBeamFailed(t)
            }
        }
    }

    private companion object {
        const val TAG = "CufflinkBeam"
    }
}
