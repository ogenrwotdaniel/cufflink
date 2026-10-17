package com.ogenrwotdaniel.cufflink.wear.ui.peek

import org.junit.Assert.assertEquals
import org.junit.Test

class NotchCounterTest {
    @Test fun smallMovementsAccumulateIntoOneNotch() {
        val n = NotchCounter(48f)
        assertEquals(0, n.feed(20f))
        assertEquals(0, n.feed(20f))
        assertEquals(1, n.feed(20f)) // 60 px: one notch, 12 px carried
        assertEquals(1, n.feed(36f)) // carry + 36 = 48
    }

    @Test fun aFastTurnYieldsSeveralNotches() {
        assertEquals(3, NotchCounter(48f).feed(150f))
    }

    @Test fun anticlockwiseIsNegativeAndCancelsCarry() {
        val n = NotchCounter(48f)
        assertEquals(0, n.feed(30f))
        assertEquals(-1, n.feed(-80f)) // net -50
        assertEquals(-2, NotchCounter(48f).feed(-100f))
    }
}
