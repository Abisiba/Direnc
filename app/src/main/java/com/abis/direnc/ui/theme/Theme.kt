package com.abis.direnc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sıcak krem palet: kâğıt zemin, koyu mürekkep yazı, tek yumuşak vurgu (toprak/terra)
private val CreamBackground = Color(0xFFF4ECDD)
private val CreamSurface = Color(0xFFFBF6EA)
private val InkPrimaryText = Color(0xFF3A352C)
private val SoftAccent = Color(0xFFB07D56)
private val OnAccent = Color(0xFFFBF6EA)
private val MutedText = Color(0xFF8A8275)

private val DirencColors = lightColorScheme(
    primary = SoftAccent,
    onPrimary = OnAccent,
    background = CreamBackground,
    onBackground = InkPrimaryText,
    surface = CreamSurface,
    onSurface = InkPrimaryText,
    surfaceVariant = CreamBackground,
    onSurfaceVariant = MutedText,
    outline = MutedText,
)

// İnce, sakin tipografi
private val DirencTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Light, fontSize = 34.sp),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Light),
    bodyLarge = Typography().bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Normal),
)

@Composable
fun DirencTheme(content: @Composable () -> Unit) {
    // Bilinçli olarak her zaman aydınlık krem tema (göz yormayan, ferah his)
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = DirencColors,
        typography = DirencTypography,
        content = content
    )
}
