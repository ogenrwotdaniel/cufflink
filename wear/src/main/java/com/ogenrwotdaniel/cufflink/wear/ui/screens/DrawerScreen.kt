package com.ogenrwotdaniel.cufflink.wear.ui.screens

import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.data.MediaShelf
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.EntryButton
import com.ogenrwotdaniel.cufflink.wear.ui.GlyphButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note
import java.io.File

/** Route for a folder screen. The path rides in the route so swipe-back climbs one level. */
fun folderRoute(dir: File) = "folder/${Uri.encode(dir.absolutePath)}"

@Composable
fun DrawerScreen(vm: CufflinkViewModel, go: (String) -> Unit) {
    val allFiles = vm.storage.hasAllFilesAccess()
    CuffList {
        item { Header("Drawer", "What's on the watch") }
        MediaShelf.entries.forEach { shelf ->
            item(key = shelf.name) { GlyphButton(shelf.glyph, shelf.title, "Media library") { go("media/${shelf.name}") } }
        }
        item { Header("Folders") }
        if (allFiles) {
            item { GlyphButton("▸", "Internal storage", "Everything, as files") { go(folderRoute(vm.storage.sharedRoot)) } }
            listOf(
                Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_ALARMS,
                Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_DOWNLOADS, Environment.DIRECTORY_DCIM,
            ).forEach { name ->
                val dir = File(vm.storage.sharedRoot, name)
                item(key = name) { GlyphButton("▸", name, dir.absolutePath, tonal = true) { go(folderRoute(dir)) } }
            }
        } else {
            item { Note("Folder browsing needs all-files access, which watches only grant over adb. See Powers.") }
            item { GlyphButton("✦", "Open Powers") { go("powers") } }
        }
    }
}

@Composable
fun MediaShelfScreen(vm: CufflinkViewModel, shelf: MediaShelf, open: (Entry) -> Unit, go: (String) -> Unit) {
    LaunchedEffect(shelf) { vm.loadShelf(shelf) }
    CuffList {
        item { Header(shelf.title) }
        if (!vm.loading && vm.listing.isEmpty()) {
            item { Note("Nothing visible. If you expected files here, allow media access in Powers.") }
            item { GlyphButton("✦", "Open Powers") { go("powers") } }
        }
        vm.listing.forEach { entry -> item(key = entry.source.toString()) { EntryButton(entry) { open(entry) } } }
    }
}

@Composable
fun FolderScreen(vm: CufflinkViewModel, dir: File, open: (Entry) -> Unit, go: (String) -> Unit) {
    LaunchedEffect(dir) { vm.loadFolder(dir) }
    CuffList {
        item { Header(dir.name.ifEmpty { "/" }, dir.parent) }
        if (!vm.loading && vm.listing.isEmpty()) item { Note("Empty folder") }
        vm.listing.forEach { entry ->
            item(key = entry.source.toString()) {
                EntryButton(entry) {
                    val file = (entry.source as? com.ogenrwotdaniel.cufflink.wear.data.Source.Disk)?.file
                    if (entry.isFolder && file != null) go(folderRoute(file)) else open(entry)
                }
            }
        }
    }
}
