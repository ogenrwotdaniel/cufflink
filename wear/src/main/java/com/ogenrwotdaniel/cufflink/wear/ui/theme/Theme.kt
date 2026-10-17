package com.ogenrwotdaniel.cufflink.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Cufflink palette: brass on midnight, with a teal "link" accent. */
object Cuff {
    val Brass = Color(0xFFE8B04B)
    val BrassDeep = Color(0xFF8A6A2C)
    val Teal = Color(0xFF3FC1B0)
    val Night = Color(0xFF0B1220)
    val Navy = Color(0xFF16213A)
    val Parchment = Color(0xFFF5EBD8)
    val Mist = Color(0xFFB9B0A0)
    val Ember = Color(0xFFFF7A6B)
}

private val scheme = ColorScheme(
    primary = Cuff.Brass,
    primaryDim = Cuff.BrassDeep,
    onPrimary = Cuff.Night,
    primaryContainer = Color(0xFF3A2E14),
    onPrimaryContainer = Color(0xFFFFE0A3),
    secondary = Cuff.Teal,
    onSecondary = Cuff.Night,
    secondaryContainer = Color(0xFF12403B),
    onSecondaryContainer = Color(0xFFBFF3EA),
    tertiary = Cuff.Parchment,
    surfaceContainerLow = Color(0xFF111A2E),
    surfaceContainer = Cuff.Navy,
    surfaceContainerHigh = Color(0xFF1E2C4A),
    onSurface = Cuff.Parchment,
    onSurfaceVariant = Cuff.Mist,
    background = Color.Black,
    onBackground = Cuff.Parchment,
    error = Cuff.Ember,
)

@Composable
fun CufflinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
