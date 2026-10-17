package com.ogenrwotdaniel.cufflink.wear.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.sound.SoundInstaller
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.GlyphButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note
import com.ogenrwotdaniel.cufflink.wear.ui.theme.Cuff

private val MEDIA = arrayOf(
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_AUDIO,
    Manifest.permission.READ_MEDIA_VIDEO,
)

/**
 * Every permission Cufflink can use, whether it has it, and how to get it.
 * Two of them (changing system sounds, all-files access) are app-ops that most
 * watches give no screen for — those show the one adb line that grants them.
 */
@Composable
fun PowersScreen(vm: CufflinkViewModel) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    // Read each time `tick` moves: after a permission dialog, or on return from Settings.
    val media = tick.let { MEDIA.all { granted(context, it) } }
    val notify = tick.let { granted(context, Manifest.permission.POST_NOTIFICATIONS) }
    val sounds = tick.let { vm.sounds.canApply() }
    val allFiles = tick.let { vm.storage.hasAllFilesAccess() }

    CuffList {
        item { Header("Powers", "What Cufflink may do") }
        item {
            GlyphButton(mark(media), "See media", "Pictures, sounds, videos", enabled = !media) { request.launch(MEDIA) }
        }
        item {
            GlyphButton(mark(notify), "Arrival alerts", "Tell me when a beam lands", enabled = !notify) {
                request.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
        item {
            GlyphButton(mark(sounds), "Change sounds", "Set ringtone & co. in one tap", enabled = !sounds) {
                openOrFail(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))
            }
        }
        if (!sounds) item { Note("No screen for it? From a computer:\n${SoundInstaller.ADB_WRITE_SETTINGS}", Cuff.Teal) }
        item {
            GlyphButton(mark(allFiles), "All files", "Browse folders, not just media", enabled = !allFiles) {
                openOrFail(context, Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${context.packageName}".toUri()))
            }
        }
        if (!allFiles) item { Note("From a computer:\n${SoundInstaller.ADB_ALL_FILES}", Cuff.Teal) }
        item { GlyphButton("⚙", "Watch sound settings", tonal = true) { openOrFail(context, Intent(Settings.ACTION_SOUND_SETTINGS)) } }
        item { Note("Without \"Change sounds\", installed sounds still appear in the watch's own Settings › Sounds picker.") }
    }
}

private fun mark(ok: Boolean) = if (ok) "✓" else "○"

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Many watch builds strip these settings screens; fall back to the main Settings app. */
private fun openOrFail(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}
