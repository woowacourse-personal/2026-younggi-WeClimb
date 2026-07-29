package com.weclimb.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

private val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

private val MaterialTypography = Typography()

internal val WeClimbTypography = Typography(
    displayLarge = MaterialTypography.displayLarge.copy(fontFamily = BarlowCondensed),
    displayMedium = MaterialTypography.displayMedium.copy(fontFamily = BarlowCondensed),
    displaySmall = MaterialTypography.displaySmall.copy(fontFamily = BarlowCondensed),
    headlineLarge = MaterialTypography.headlineLarge.copy(fontFamily = BarlowCondensed),
    headlineMedium = MaterialTypography.headlineMedium.copy(fontFamily = BarlowCondensed),
    headlineSmall = MaterialTypography.headlineSmall.copy(fontFamily = BarlowCondensed),
    titleLarge = MaterialTypography.titleLarge.copy(fontFamily = BarlowCondensed),
    titleMedium = MaterialTypography.titleMedium.copy(fontFamily = Barlow),
    titleSmall = MaterialTypography.titleSmall.copy(fontFamily = Barlow),
    bodyLarge = MaterialTypography.bodyLarge.copy(fontFamily = Barlow),
    bodyMedium = MaterialTypography.bodyMedium.copy(fontFamily = Barlow),
    bodySmall = MaterialTypography.bodySmall.copy(fontFamily = Barlow),
    labelLarge = MaterialTypography.labelLarge.copy(fontFamily = Barlow),
    labelMedium = MaterialTypography.labelMedium.copy(fontFamily = Barlow),
    labelSmall = MaterialTypography.labelSmall.copy(fontFamily = Barlow),
)

@Composable
fun WeClimbTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WeClimbColorScheme,
        typography = WeClimbTypography,
        content = content,
    )
}
