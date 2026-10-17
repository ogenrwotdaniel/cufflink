package com.ogenrwotdaniel.cufflink.wear.ui.peek

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

/** Zoom and pan of a [ZoomableBitmap], hoisted so the bezel can drive it too. */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    fun zoomBy(factor: Float) {
        scale = (scale * factor).coerceIn(MIN, MAX)
        if (scale <= MIN) offset = Offset.Zero
    }

    fun panBy(delta: Offset) {
        offset = if (scale > MIN) offset + delta else Offset.Zero
    }

    fun toggle() {
        if (scale > MIN) reset() else scale = 2.5f
    }

    fun reset() {
        scale = MIN
        offset = Offset.Zero
    }

    private companion object {
        const val MIN = 1f
        const val MAX = 6f
    }
}

/** A bitmap you can pinch, drag and double-tap. Single taps report which half was hit. */
@Composable
fun ZoomableBitmap(
    bitmap: Bitmap,
    contentDescription: String,
    state: ZoomState,
    modifier: Modifier = Modifier,
    onTapSide: ((left: Boolean) -> Unit)? = null,
) {
    Image(
        bitmap = remember(bitmap) { bitmap.asImageBitmap() },
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state) {
                detectTransformGestures { _, pan, zoom, _ ->
                    state.zoomBy(zoom)
                    state.panBy(pan)
                }
            }
            .pointerInput(state, onTapSide) {
                detectTapGestures(
                    onDoubleTap = { state.toggle() },
                    onTap = { pos -> onTapSide?.invoke(pos.x < size.width / 2) },
                )
            }
            .graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.offset.x
                translationY = state.offset.y
            },
    )
}
