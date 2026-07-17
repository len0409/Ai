package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentOrange,
    background = DarkBg,
    surface = CardBg,
    surfaceVariant = CardBgHover,
    onPrimary = DarkBg,
    onSecondary = DarkBg,
    onTertiary = DarkBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    error = AccentRed
)

@Composable
fun AiRelayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

val Typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, fontFamily = FontFamily.Default),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 13.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
)
