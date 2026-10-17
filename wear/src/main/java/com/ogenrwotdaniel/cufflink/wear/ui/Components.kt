package com.ogenrwotdaniel.cufflink.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.ogenrwotdaniel.cufflink.shared.FileKind
import com.ogenrwotdaniel.cufflink.shared.Sizes
import com.ogenrwotdaniel.cufflink.wear.data.Entry
import com.ogenrwotdaniel.cufflink.wear.ui.theme.Cuff

/** The standard Cufflink screen: a curved, rotary-scrollable list under the time. */
@Composable
fun CuffList(content: TransformingLazyColumnScope.() -> Unit) {
    val state = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(state = state, contentPadding = padding, content = content)
    }
}

@Composable
fun Header(title: String, subtitle: String? = null) {
    ListHeader {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Cuff.Brass, textAlign = TextAlign.Center)
    }
    if (subtitle != null) {
        Note(subtitle)
    }
}

/** Small centred body text — hints, empty states, explanations. */
@Composable
fun Note(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.Center,
    )
}

/** A navigation chip with a glyph "icon" — Cufflink draws its icons in type. */
@Composable
fun GlyphButton(
    glyph: String,
    label: String,
    secondary: String? = null,
    tonal: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = if (tonal) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.filledVariantButtonColors(),
        icon = { Text(glyph, style = MaterialTheme.typography.titleMedium, color = Cuff.Brass) },
        secondaryLabel = secondary?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
fun EntryButton(entry: Entry, onClick: () -> Unit) {
    val detail = when (entry.kind) {
        FileKind.FOLDER -> "${entry.sizeBytes} items"
        else -> "${entry.kind.label} · ${Sizes.format(entry.sizeBytes)}"
    }
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = { Text(entry.kind.glyph, style = MaterialTheme.typography.titleMedium, color = glyphColor(entry.kind)) },
        secondaryLabel = { Text(detail, maxLines = 1) },
        label = { Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

fun glyphColor(kind: FileKind): Color = when (kind) {
    FileKind.AUDIO -> Cuff.Teal
    FileKind.FOLDER -> Cuff.Brass
    FileKind.IMAGE -> Color(0xFFFFB38A)
    FileKind.VIDEO -> Color(0xFFB59CFF)
    else -> Cuff.Parchment
}

/**
 * A ring drawn just inside the round screen's edge — Cufflink's signature
 * "bezel gauge". Used for storage on Home and playback progress in the player.
 */
@Composable
fun BezelGauge(fraction: Float, modifier: Modifier = Modifier, color: Color = Cuff.Brass, track: Color = Cuff.Navy) {
    Canvas(modifier.fillMaxSize()) {
        val stroke = 5.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
