package com.ogenrwotdaniel.cufflink.wear.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.EntryButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note

/** Files beamed from the phone, newest first. Private to Cufflink until saved out. */
@Composable
fun PocketScreen(vm: CufflinkViewModel, open: (Entry) -> Unit) {
    LaunchedEffect(Unit) { vm.refreshHome() }
    CuffList {
        item { Header("Pocket", "Beamed from your phone") }
        if (vm.pocket.isEmpty()) {
            item { Note("Empty for now. On your phone: Cufflink › Beam files, or Share › Beam to watch.") }
        }
        vm.pocket.forEach { entry -> item(key = entry.name) { EntryButton(entry) { open(entry) } } }
    }
}
