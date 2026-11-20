package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val CyberiDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberVoid,
    primaryContainer = CyberSurfaceElevated,
    onPrimaryContainer = CyberCyanSoft,
    secondary = CyberBlue,
    onSecondary = TextPrimary,
    secondaryContainer = CyberSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = CyberEmerald,
    onTertiary = CyberVoid,
    background = CyberVoid,
    onBackground = TextPrimary,
    surface = CyberDeepNavy,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = TextSecondary,
    error = CyberCrimson,
    onError = TextPrimary,
    outline = GlassBorderBright,
    outlineVariant = GlassBorderSubtle
)

val CyberiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun CyberiTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = CyberiDarkColorScheme,
            typography = CyberiTypography,
            shapes = CyberiShapes,
            content = content
        )
    }
}
