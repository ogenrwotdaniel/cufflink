package com.ogenrwotdaniel.cufflink.wear.ui.peek

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ogenrwotdaniel.cufflink.shared.FileKind
import com.ogenrwotdaniel.cufflink.shared.Sizes
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.sound.SoundInstaller
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.GlyphButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note
import com.ogenrwotdaniel.cufflink.wear.ui.theme.Cuff
import kotlinx.coroutines.launch
import java.util.Date

/** Everything you can do with one file: details, make it a sound, save it out, send it back, delete. */
@Composable
fun ActionsScreen(vm: CufflinkViewModel, onGone: () -> Unit) {
    val entry = vm.selected ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember(entry) { mutableStateOf(false) }
    LaunchedEffect(entry) { vm.clearStatus() }

    CuffList {
        item { Header(entry.name, "${entry.kind.label} · ${Sizes.format(entry.sizeBytes)}") }
        vm.status?.let { s -> item { Note(s, if (s.startsWith("✓")) Cuff.Teal else Cuff.Parchment) } }
        if (entry.kind == FileKind.AUDIO) {
            listOf(SoundRole.RINGTONE, SoundRole.NOTIFICATION, SoundRole.ALARM).forEach { role ->
                item(key = role.name) {
                    GlyphButton("♪", "Set as ${SoundInstaller.label(role).lowercase()}") { vm.installSound(entry, role) }
                }
            }
        }
        if (entry.inPocket) {
            item { GlyphButton("⇩", "Save to watch storage", "So other watch apps see it", tonal = true) { vm.saveToShared(entry) } }
        }
        item { GlyphButton("⇄", "Send to phone", "Lands in Downloads/Cufflink", tonal = true) { vm.sendToPhone(entry) } }
        item {
            GlyphButton("✕", if (confirmDelete) "Tap again to delete" else "Delete", tonal = !confirmDelete) {
                if (!confirmDelete) confirmDelete = true
                else scope.launch { if (vm.delete(entry)) onGone() }
            }
        }
        item {
            val date = DateFormat.getMediumDateFormat(context).format(Date(entry.modifiedMillis))
            Note("$date\n${entry.location}")
        }
    }
}
