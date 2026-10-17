package com.ogenrwotdaniel.cufflink.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Brass on midnight, teal for the link. Same palette as the watch. */
object Cuff {
    val Brass = Color(0xFFE8B04B)
    val Teal = Color(0xFF3FC1B0)
    val Night = Color(0xFF0B1220)
    val Navy = Color(0xFF16213A)
    val Parchment = Color(0xFFF5EBD8)
    val Mist = Color(0xFFB9B0A0)
    val Ember = Color(0xFFFF7A6B)
}

// Written out in full: a scheme given only a few roles keeps Material's purple
// baseline for containers and surfaces, which is what cards are made of.
private val scheme = darkColorScheme(
    primary = Cuff.Brass,
    onPrimary = Cuff.Night,
    primaryContainer = Color(0xFF3A2E14),
    onPrimaryContainer = Color(0xFFFFE0A3),
    inversePrimary = Color(0xFF7A5A1C),
    secondary = Cuff.Teal,
    onSecondary = Cuff.Night,
    secondaryContainer = Color(0xFF12403B),
    onSecondaryContainer = Color(0xFFBFF3EA),
    tertiary = Cuff.Parchment,
    onTertiary = Cuff.Night,
    tertiaryContainer = Color(0xFF3B352B),
    onTertiaryContainer = Cuff.Parchment,
    background = Cuff.Night,
    onBackground = Cuff.Parchment,
    surface = Cuff.Night,
    onSurface = Cuff.Parchment,
    surfaceVariant = Color(0xFF1E2C4A),
    onSurfaceVariant = Cuff.Mist,
    surfaceTint = Cuff.Brass,
    inverseSurface = Cuff.Parchment,
    inverseOnSurface = Cuff.Night,
    error = Cuff.Ember,
    onError = Cuff.Night,
    errorContainer = Color(0xFF4A1A16),
    onErrorContainer = Color(0xFFFFDAD5),
    outline = Color(0xFF4A5875),
    outlineVariant = Color(0xFF2A3754),
    scrim = Color.Black,
    surfaceBright = Color(0xFF2A3754),
    surfaceDim = Cuff.Night,
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF111A2E),
    surfaceContainer = Cuff.Navy,
    surfaceContainerHigh = Color(0xFF1E2C4A),
    surfaceContainerHighest = Color(0xFF26355A),
)

@Composable
fun CufflinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
