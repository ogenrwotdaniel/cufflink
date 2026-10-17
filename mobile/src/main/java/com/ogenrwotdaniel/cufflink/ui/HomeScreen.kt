package com.ogenrwotdaniel.cufflink.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ogenrwotdaniel.cufflink.R
import com.ogenrwotdaniel.cufflink.beam.Arrival
import com.ogenrwotdaniel.cufflink.beam.BeamQueue
import com.ogenrwotdaniel.cufflink.beam.Transfer
import com.ogenrwotdaniel.cufflink.shared.BeamTransport
import com.ogenrwotdaniel.cufflink.shared.Sizes
import com.ogenrwotdaniel.cufflink.shared.SoundRole
import kotlinx.coroutines.launch

/** What the watch link looks like right now. */
private sealed interface WatchLink {
    data object Checking : WatchLink
    data object NoWatch : WatchLink
    data class NoApp(val watchName: String) : WatchLink
    data class Ready(val watchName: String) : WatchLink
}

@Composable
fun HomeScreen(shared: List<Uri>, onSharedHandled: () -> Unit, padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transfers by BeamQueue.transfers.collectAsStateWithLifecycle()
    val arrivals by BeamQueue.arrivals.collectAsStateWithLifecycle()

    var link by remember { mutableStateOf<WatchLink>(WatchLink.Checking) }
    val refresh: () -> Unit = {
        scope.launch {
            link = WatchLink.Checking
            link = runCatching { checkLink(context) }.getOrDefault(WatchLink.NoWatch)
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) BeamQueue.beam(context, uris)
    }
    var pendingRole by remember { mutableStateOf(SoundRole.RINGTONE) }
    val pickSound = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) BeamQueue.beam(context, listOf(uri), pendingRole)
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Hero() }
        item { Section { LinkCard(link, refresh) } }

        if (shared.isNotEmpty()) {
            item {
                Section {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ready to beam", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (shared.size == 1) shared[0].lastPathSegment ?: "1 file" else "${shared.size} files",
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Button(onClick = { BeamQueue.beam(context, shared); onSharedHandled() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Beam to watch")
                            }
                            if (shared.size == 1) {
                                Text("…or make it a watch sound:", style = MaterialTheme.typography.bodySmall)
                                RoleRow { role -> BeamQueue.beam(context, shared, role); onSharedHandled() }
                            }
                            TextButton(onClick = onSharedHandled) { Text("Never mind") }
                        }
                    }
                }
            }
        }

        item {
            Section {
                Button(
                    onClick = { pickFiles.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                ) { Text("Beam files to watch", style = MaterialTheme.typography.titleMedium) }
            }
        }

        item {
            Section {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Make a watch sound", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pick any audio file. Cufflink sends it, files it under the right system folder and — if " +
                                "\"Change sounds\" is allowed on the watch — switches it on. No watch tapping needed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        RoleRow { role -> pendingRole = role; pickSound.launch(arrayOf("audio/*")) }
                    }
                }
            }
        }

        if (transfers.isNotEmpty()) {
            item {
                Section {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("On the wire", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { BeamQueue.clearFinished() }) { Text("Clear done") }
                    }
                }
            }
            items(transfers.reversed(), key = { it.id }) { Section { TransferRow(it) } }
        }

        if (arrivals.isNotEmpty()) {
            item { Section { Text("From your watch", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth()) } }
            items(arrivals, key = { it.uri.toString() }) { Section { ArrivalRow(it) } }
        }

        item { Section { PowersCard() } }
        item {
            Text(
                "Cufflink is open source · Apache-2.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Caps content at a readable width on a tablet without fighting the fill (cap first, then fill). */
@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) { content() }
}

@Composable
private fun Hero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(112.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
        )
        Spacer(Modifier.height(8.dp))
        Text("Cufflink", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, color = Cuff.Brass)
        Text("Files for your wrist", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinkCard(link: WatchLink, refresh: () -> Unit) {
    val (dot, title, body) = when (link) {
        WatchLink.Checking -> Triple(Cuff.Mist, "Looking for your watch…", "")
        WatchLink.NoWatch -> Triple(Cuff.Ember, "No watch connected", "Pair it in Galaxy Wearable and keep Bluetooth on.")
        is WatchLink.NoApp -> Triple(Cuff.Brass, link.watchName, "Connected, but Cufflink isn't on the watch yet. Install the wear APK (see README).")
        is WatchLink.Ready -> Triple(Cuff.Teal, link.watchName, "Linked and ready to catch files.")
    }
    Card {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(12.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (body.isNotEmpty()) Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = refresh) { Text("Check") }
        }
    }
}

@Composable
private fun RoleRow(onRole: (SoundRole) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(SoundRole.RINGTONE to "Ringtone", SoundRole.NOTIFICATION to "Notification", SoundRole.ALARM to "Alarm").forEach { (role, label) ->
            FilledTonalButton(onClick = { onRole(role) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TransferRow(t: Transfer) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                val badge = when (t.state) {
                    Transfer.State.WAITING -> "queued"
                    Transfer.State.SENDING -> "${(Sizes.fraction(t.sentBytes, t.sizeBytes) * 100).toInt()}%"
                    Transfer.State.DONE -> "✓"
                    Transfer.State.FAILED -> "✕"
                }
                Text(badge, color = if (t.state == Transfer.State.FAILED) Cuff.Ember else Cuff.Teal, style = MaterialTheme.typography.labelLarge)
            }
            if (t.state == Transfer.State.SENDING) {
                LinearProgressIndicator(progress = { Sizes.fraction(t.sentBytes, t.sizeBytes) }, modifier = Modifier.fillMaxWidth())
            }
            val role = if (t.role == SoundRole.NONE) "" else " · as ${t.role.name.lowercase()}"
            Text(
                "${Sizes.format(t.sizeBytes)}$role${t.message?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArrivalRow(a: Arrival) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Sizes.format(a.sizeBytes)} · Downloads/Cufflink", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = {
                val view = Intent(Intent.ACTION_VIEW).setData(a.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                runCatching { context.startActivity(Intent.createChooser(view, a.name)) }
            }) { Text("Open") }
        }
    }
}

/** The two watch permissions with no on-watch screen, as copyable adb lines. */
@Composable
private fun PowersCard() {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Unlock watch powers", style = MaterialTheme.typography.titleMedium)
            Text(
                "Galaxy watches hide two settings Cufflink can use. Grant them once from a computer with adb " +
                    "(watch: Settings › Developer options › Wireless debugging).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(
                "Set sounds in one tap" to "adb shell appops set com.ogenrwotdaniel.cufflink WRITE_SETTINGS allow",
                "Browse every folder" to "adb shell appops set com.ogenrwotdaniel.cufflink MANAGE_EXTERNAL_STORAGE allow",
            ).forEach { (label, cmd) ->
                Text(label, style = MaterialTheme.typography.labelLarge, color = Cuff.Brass)
                Text(
                    cmd,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(10.dp),
                )
                OutlinedButton(onClick = { copy(context, cmd) }) { Text("Copy") }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("adb", text))
}

private suspend fun checkLink(context: Context): WatchLink {
    val transport = BeamTransport(context)
    val peers = transport.peers()
    peers.firstOrNull()?.let { return WatchLink.Ready(it.displayName) }
    val any = transport.connectedNodes().firstOrNull() ?: return WatchLink.NoWatch
    return WatchLink.NoApp(any.displayName)
}
