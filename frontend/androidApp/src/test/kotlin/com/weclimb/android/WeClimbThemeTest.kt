package com.weclimb.android

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class WeClimbThemeTest {
    @Test
    fun `uses the frozen dark design bundle palette`() {
        assertEquals(Color(0xFFEA580C), WeClimbColorScheme.primary)
        assertEquals(Color(0xFF059669), WeClimbColorScheme.secondary)
        assertEquals(Color(0xFF0B0F1A), WeClimbColorScheme.background)
        assertEquals(Color(0xFF141A26), WeClimbColorScheme.surface)
        assertEquals(Color(0xFFDC2626), WeClimbColorScheme.error)
        assertEquals(Color(0xFF5A8FDB), WeClimbHoldColors.blue)
        assertEquals(Color(0xFF46AE72), WeClimbHoldColors.green)
    }
}
