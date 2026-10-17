package com.ogenrwotdaniel.cufflink.wear.ui.peek

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ogenrwotdaniel.cufflink.shared.FileKind
import com.ogenrwotdaniel.cufflink.wear.CufflinkViewModel
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.data.Source
import com.ogenrwotdaniel.cufflink.wear.ui.BezelGauge
import com.ogenrwotdaniel.cufflink.wear.ui.CuffList
import com.ogenrwotdaniel.cufflink.wear.ui.GlyphButton
import com.ogenrwotdaniel.cufflink.wear.ui.Header
import com.ogenrwotdaniel.cufflink.wear.ui.Note
import com.ogenrwotdaniel.cufflink.wear.ui.theme.Cuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** Opens the right viewer for the selected file; anything unviewable goes straight to its actions. */
@Composable
fun PeekScreen(vm: CufflinkViewModel, onActions: () -> Unit) {
    val entry = vm.selected ?: return CenterNote("Nothing selected")
    when (entry.kind) {
        FileKind.IMAGE -> ImagePeek(vm, entry, onActions)
        FileKind.AUDIO -> AudioPeek(vm, entry, onActions)
        FileKind.VIDEO -> VideoPeek(entry, onActions)
        FileKind.TEXT -> TextPeek(vm, entry, onActions)
        FileKind.PDF -> PdfPeek(vm, entry, onActions)
        else -> LaunchedEffect(entry) { onActions() }
    }
}

/** Longest edge a decoded picture is allowed — about 2.5× a watch screen, plenty for zooming. */
private const val MAX_DECODE_PX = 1200

@Composable
private fun ImagePeek(vm: CufflinkViewModel, entry: Entry, onActions: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(entry) { mutableStateOf<Bitmap?>(null) }
    var error by remember(entry) { mutableStateOf<String?>(null) }
    LaunchedEffect(entry) {
        withContext(Dispatchers.IO) {
            runCatching {
                val source = when (val s = entry.source) {
                    is Source.Disk -> ImageDecoder.createSource(s.file)
                    is Source.Media -> ImageDecoder.createSource(context.contentResolver, s.uri)
                }
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > MAX_DECODE_PX) decoder.setTargetSampleSize((longest / MAX_DECODE_PX).coerceAtLeast(1))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.onSuccess { bitmap = it }.onFailure { error = it.message ?: "Can't decode" }
        }
    }
    val zoom = remember(entry) { ZoomState() }
    val notches = remember { NotchCounter(24f) }
    PeekFrame(onActions) {
        val b = bitmap
        when {
            // Turn the bezel to zoom: clockwise in, anticlockwise out.
            b != null -> ZoomableBitmap(
                b, entry.name, zoom,
                modifier = Modifier.rotary { px ->
                    val n = notches.feed(px)
                    if (n != 0) zoom.zoomBy(Math.pow(1.15, n.toDouble()).toFloat())
                },
            )
            error != null -> CenterNote("Can't show this picture.\n$error")
            else -> CenterNote("Developing…")
        }
    }
}

@Composable
private fun AudioPeek(vm: CufflinkViewModel, entry: Entry, onActions: () -> Unit) {
    val context = LocalContext.current
    val player = remember(entry) { MediaPlayer() }
    var ready by remember(entry) { mutableStateOf(false) }
    var playing by remember(entry) { mutableStateOf(false) }
    var position by remember(entry) { mutableIntStateOf(0) }
    var duration by remember(entry) { mutableIntStateOf(0) }
    var error by remember(entry) { mutableStateOf<String?>(null) }

    DisposableEffect(entry) {
        runCatching {
            when (val s = entry.source) {
                is Source.Disk -> player.setDataSource(s.file.absolutePath)
                is Source.Media -> player.setDataSource(context, s.uri)
            }
            player.setOnPreparedListener { duration = it.duration; ready = true; it.start(); playing = true }
            player.setOnCompletionListener { playing = false; position = duration }
            player.prepareAsync()
        }.onFailure { error = it.message ?: "Can't play" }
        onDispose { player.release() }
    }
    LaunchedEffect(playing) {
        while (playing) {
            position = runCatching { player.currentPosition }.getOrDefault(position)
            delay(250)
        }
    }
    val notches = remember { NotchCounter() }

    Box(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black).rotary { px ->
            // Turn the bezel to scrub: one notch = 5 seconds.
            val n = notches.feed(px)
            if (n != 0 && ready) {
                position = (position + n * 5_000).coerceIn(0, duration)
                player.seekTo(position)
            }
        },
    ) {
        BezelGauge(if (duration > 0) position.toFloat() / duration else 0f, color = Cuff.Teal)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                entry.name.substringBeforeLast('.'),
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall, color = Cuff.Parchment,
            )
            Text(
                error ?: "${clock(position)} / ${clock(duration)}",
                style = MaterialTheme.typography.bodySmall, color = Cuff.Mist,
            )
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Cuff.Brass).clickable(enabled = ready) {
                    if (player.isPlaying) { player.pause(); playing = false } else {
                        if (position >= duration) player.seekTo(0)
                        player.start(); playing = true
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (playing) "❚❚" else "▶", color = Cuff.Night, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Set as sound ›",
                modifier = Modifier.clip(CircleShape).clickable(onClick = onActions).padding(8.dp),
                color = Cuff.Brass, style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun VideoPeek(entry: Entry, onActions: () -> Unit) {
    var error by remember(entry) { mutableStateOf<String?>(null) }
    PeekFrame(onActions) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    when (val s = entry.source) {
                        is Source.Disk -> setVideoPath(s.file.absolutePath)
                        is Source.Media -> setVideoURI(s.uri)
                    }
                    setOnPreparedListener { start() }
                    setOnErrorListener { _, what, _ -> error = "Can't play this video ($what)"; true }
                    setOnClickListener { if (isPlaying) pause() else start() }
                }
            },
            onRelease = { it.stopPlayback() },
        )
        error?.let { CenterNote(it) }
    }
}

/** Show at most this much of a text file. A watch is for glancing, not for reading logs. */
private const val TEXT_LIMIT = 64 * 1024

@Composable
private fun TextPeek(vm: CufflinkViewModel, entry: Entry, onActions: () -> Unit) {
    var chunks by remember(entry) { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(entry) {
        chunks = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = vm.storage.openInput(entry).use { it.readNBytes(TEXT_LIMIT) }
                val text = String(bytes, Charsets.UTF_8)
                // ~400-character pieces split on line breaks: small enough to scroll smoothly.
                val out = mutableListOf<String>()
                val current = StringBuilder()
                text.lineSequence().forEach { line ->
                    if (current.length + line.length > 400 && current.isNotEmpty()) {
                        out += current.toString(); current.clear()
                    }
                    current.append(line).append('\n')
                }
                if (current.isNotBlank()) out += current.toString().trimEnd()
                if (entry.sizeBytes > TEXT_LIMIT) out += "… (first 64 KB shown)"
                out
            }.getOrElse { listOf("Can't read: ${it.message}") }
        }
    }
    CuffList {
        item { Header(entry.name) }
        val list = chunks
        if (list == null) item { Note("Unfolding…") }
        list?.forEachIndexed { i, chunk ->
            item(key = i) {
                Text(
                    chunk,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Cuff.Parchment,
                )
            }
        }
        item { GlyphButton("⋯", "File actions", tonal = true) { onActions() } }
    }
}

@Composable
private fun PdfPeek(vm: CufflinkViewModel, entry: Entry, onActions: () -> Unit) {
    val widthPx = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.coerceAtLeast(320) }
    var renderer by remember(entry) { mutableStateOf<PdfRenderer?>(null) }
    var page by remember(entry) { mutableIntStateOf(0) }
    var bitmap by remember(entry) { mutableStateOf<Bitmap?>(null) }
    var error by remember(entry) { mutableStateOf<String?>(null) }

    DisposableEffect(entry) {
        runCatching { PdfRenderer(vm.storage.openFd(entry)) }
            .onSuccess { renderer = it }
            .onFailure { error = it.message ?: "Can't open PDF" }
        onDispose { renderer?.close() }
    }
    LaunchedEffect(renderer, page) {
        val r = renderer ?: return@LaunchedEffect
        // PdfRenderer is single-threaded; render on the main thread, it is one page.
        r.openPage(page).use { p ->
            val scale = (widthPx * 2f) / p.width
            val bmp = createBitmap((p.width * scale).toInt(), (p.height * scale).toInt())
            bmp.eraseColor(AndroidColor.WHITE)
            p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap = bmp
        }
    }
    val count = renderer?.pageCount ?: 0
    fun turn(by: Int) {
        if (count > 0) page = (page + by).coerceIn(0, count - 1)
    }
    val notches = remember { NotchCounter() }
    val zoom = remember(entry) { ZoomState() }
    LaunchedEffect(page) { zoom.reset() }
    PeekFrame(onActions) {
        val b = bitmap
        when {
            error != null -> CenterNote("Can't show this PDF.\n$error")
            b != null -> ZoomableBitmap(
                b, "Page ${page + 1}", zoom,
                modifier = Modifier.rotary { px -> turn(notches.feed(px)) },
                onTapSide = { left -> turn(if (left) -1 else 1) },
            )
            else -> CenterNote("Turning pages…")
        }
        if (count > 0) {
            Text(
                "${page + 1} / $count",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                    .clip(CircleShape).background(Cuff.Navy.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = Cuff.Brass,
            )
        }
    }
}

private fun clock(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)
}
