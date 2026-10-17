package com.ogenrwotdaniel.cufflink.wear.ui.peek

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent

/**
 * Route the bezel (the Ultra's touch bezel, or a rotating crown) to [onScroll],
 * in raw pixels — positive is clockwise. The modifier grabs focus on entry,
 * because rotary events only go to the focused node.
 */
@Composable
fun Modifier.rotary(onScroll: (Float) -> Unit): Modifier {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    return this
        .onRotaryScrollEvent { onScroll(it.verticalScrollPixels); true }
        .focusRequester(focus)
        .focusable()
}

/**
 * Turns a stream of rotary pixels into whole "notches", so one deliberate turn of
 * the bezel is one page or one seek step rather than a jitter of tiny ones.
 */
class NotchCounter(private val pixelsPerNotch: Float = 48f) {
    private var carry = 0f

    fun feed(pixels: Float): Int {
        carry += pixels
        val notches = (carry / pixelsPerNotch).toInt()
        carry -= notches * pixelsPerNotch
        return notches
    }
}
