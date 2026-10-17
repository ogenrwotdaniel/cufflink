package com.ogenrwotdaniel.cufflink.wear.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ogenrwotdaniel.cufflink.shared.Sizes
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.ui.BezelGauge
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.GlyphButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note

@Composable
fun HomeScreen(vm: CufflinkViewModel, go: (String) -> Unit) {
    LaunchedEffect(Unit) { vm.refreshHome() }
    val stats = vm.stats
    Box(Modifier.fillMaxSize()) {
        BezelGauge(stats.usedFraction)
        CuffList {
            item { Header("Cufflink", "${Sizes.format(stats.freeBytes)} free of ${Sizes.format(stats.totalBytes)}") }
            item {
                val n = vm.pocket.size
                GlyphButton("◈", "Pocket", if (n == 0) "Beams from your phone" else "$n beamed file${if (n == 1) "" else "s"}") { go("pocket") }
            }
            item { GlyphButton("♪", "Sound Shelf", "Ringtone · notification · alarm") { go("shelf") } }
            item { GlyphButton("▤", "Drawer", "Browse the watch") { go("drawer") } }
            item { GlyphButton("✦", "Powers", "Permissions & adb tricks", tonal = true) { go("powers") } }
            item { Note("Beam files here from the Cufflink phone app, or its share sheet.") }
        }
    }
}
