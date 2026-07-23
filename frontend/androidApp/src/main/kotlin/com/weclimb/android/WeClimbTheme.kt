package com.weclimb.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal data class HoldColorTokens(
    val red: Color = Color(0xFFE45D5D),
    val orange: Color = Color(0xFFE68A4C),
    val yellow: Color = Color(0xFFDDBB44),
    val green: Color = Color(0xFF46AE72),
    val blue: Color = Color(0xFF5A8FDB),
    val indigo: Color = Color(0xFF7C7FDB),
    val purple: Color = Color(0xFFA877D2),
)

internal val WeClimbHoldColors = HoldColorTokens()

internal val WeClimbColorScheme = darkColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    secondary = Color(0xFF059669),
    error = Color(0xFFDC2626),
    background = Color(0xFF0B0F1A),
    surface = Color(0xFF141A26),
    surfaceVariant = Color(0xFF1E2634),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0x1AFFFFFF),
)

@Composable
fun WeClimbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WeClimbColorScheme, content = content)
}
