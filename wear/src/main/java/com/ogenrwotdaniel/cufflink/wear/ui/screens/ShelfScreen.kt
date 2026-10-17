package com.ogenrwotdaniel.cufflink.wear.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.sound.SoundInstaller
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.EntryButton
import com.ogenrwotdaniel.cufflink.wear.ui.GlyphButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note

/**
 * The sounds the watch is using now (tap to hear one), then every sound it could
 * use instead. Tapping a sound opens the player, which carries the "Set as" actions.
 */
@Composable
fun ShelfScreen(vm: CufflinkViewModel, open: (Entry) -> Unit, go: (String) -> Unit) {
    val context = LocalContext.current
    val canReadLibrary = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    LaunchedEffect(canReadLibrary) { vm.loadSounds(includeLibrary = canReadLibrary) }

    var playing by remember { mutableStateOf<Ringtone?>(null) }
    DisposableEffect(Unit) { onDispose { playing?.stop() } }

    CuffList {
        item { Header("Sound Shelf", "Now playing on this watch") }
        listOf(SoundRole.RINGTONE, SoundRole.NOTIFICATION, SoundRole.ALARM).forEach { role ->
            item(key = role.name) {
                GlyphButton(
                    glyph = "◉",
                    label = SoundInstaller.label(role),
                    secondary = vm.sounds.currentTitle(role) ?: "Silent / unknown",
                    tonal = true,
                ) {
                    playing?.stop()
                    val uri = vm.sounds.currentUri(role) ?: return@GlyphButton
                    playing = RingtoneManager.getRingtone(context, uri)?.also { it.play() }
                }
            }
        }
        item { Header("Your sounds") }
        if (!canReadLibrary) {
            item { Note("Showing Pocket sounds only. Allow media access in Powers to see the watch's music too.") }
            item { GlyphButton("✦", "Open Powers") { go("powers") } }
        }
        if (!vm.loading && vm.listing.isEmpty()) {
            item { Note("No sounds yet. Beam an mp3/ogg from your phone — pick \"Ringtone\" there and it installs itself.") }
        }
        vm.listing.forEach { entry ->
            item(key = entry.source.toString()) {
                EntryButton(entry) {
                    playing?.stop()
                    open(entry)
                }
            }
        }
    }
}
