package com.ogenrwotdaniel.cufflink.wear.beam

import com.ogenrwotdaniel.cufflink.shared.BeamListenerService
import com.ogenrwotdaniel.cufflink.shared.FileKind
import com.ogenrwotdaniel.cufflink.shared.ReceivedBeam
import com.ogenrwotdaniel.cufflink.shared.Sizes
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import com.ogenrwotdaniel.cufflink.wear.data.WatchStorage
import com.ogenrwotdaniel.cufflink.wear.sound.SoundInstaller
import java.io.File

/**
 * Catches files beamed from the phone and drops them in the Pocket.
 *
 * A beam marked with a [SoundRole] ("make this my ringtone" on the phone) is also
 * installed straight away — applied if WRITE_SETTINGS is granted, otherwise staged
 * into the system picker with a notification that finishes the job in one tap.
 */
class PocketListenerService : BeamListenerService() {

    override fun inboxFolder(): File = WatchStorage(this).pocketDir

    override suspend fun onBeamArrived(beam: ReceivedBeam) {
        PocketEvents.arrived()
        val name = beam.file.name
        val role = beam.header.role
        if (role == SoundRole.NONE || FileKind.of(name, beam.header.mimeType) != FileKind.AUDIO) {
            BeamNotifications.arrived(this, name, "${FileKind.of(name, beam.header.mimeType).label} · ${Sizes.format(beam.file.length())}")
            return
        }
        val entry = WatchStorage(this).pocketFile(name) ?: return
        val label = SoundInstaller.label(role)
        val text = runCatching { SoundInstaller(this).install(entry, role) }.fold(
            onSuccess = {
                when (it) {
                    is SoundInstaller.Outcome.Applied -> "Now your $label sound"
                    is SoundInstaller.Outcome.Staged -> "Ready in Settings › Sounds — tap to finish"
                }
            },
            onFailure = { "Could not install: ${it.message}" },
        )
        BeamNotifications.arrived(this, name, "$label · $text")
    }

    override fun onBeamFailed(error: Throwable) {
        BeamNotifications.failed(this, error.message ?: "Transfer interrupted")
    }
}
