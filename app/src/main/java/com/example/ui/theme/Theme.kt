package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class CyberiColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyan: Color,
    val blue: Color,
    val indigo: Color,
    val emerald: Color,
    val crimson: Color,
    val amber: Color
)

private val LightCyberiColors = CyberiColors(
    isDark = false,
    bgTop = LightBackground,
    bgBottom = LightBackgroundSecondary,
    surface = LightSurface,
    glassFillTop = LightSurfaceCardTop,
    glassFillBottom = LightSurfaceCard,
    glassBorderSubtle = LightGlassBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    cyan = CyberCyanLight,
    blue = CyberBlueLight,
    indigo = CyberIndigoLight,
    emerald = CyberEmeraldLight,
    crimson = CyberCrimsonLight,
    amber = CyberAmberLight
)

private val DarkCyberiColors = CyberiColors(
    isDark = true,
    bgTop = DarkBackground,
    bgBottom = DarkBackgroundSecondary,
    surface = DarkSurface,
    glassFillTop = DarkSurfaceCardTop,
    glassFillBottom = DarkSurfaceCard,
    glassBorderSubtle = DarkGlassBorder,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    cyan = CyberCyanDark,
    blue = CyberBlueDark,
    indigo = CyberIndigoDark,
    emerald = CyberEmeraldDark,
    crimson = CyberCrimsonDark,
    amber = CyberAmberDark
)

val LocalCyberiColors = staticCompositionLocalOf { LightCyberiColors }

private val CyberiLightColorScheme = lightColorScheme(
    primary = CyberCyanLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = LightTextPrimary,
    secondary = CyberBlueLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    onSecondaryContainer = LightTextPrimary,
    tertiary = CyberEmeraldLight,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFEDF2FA),
    onSurfaceVariant = LightTextSecondary,
    error = CyberCrimsonLight,
    onError = Color.White,
    outline = LightGlassBorder,
    outlineVariant = LightGlassBorder
)

private val CyberiDarkColorScheme = darkColorScheme(
    primary = CyberCyanDark,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurface,
    onPrimaryContainer = CyberCyanDark,
    secondary = CyberBlueDark,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = CyberEmeraldDark,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkBackgroundSecondary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    error = CyberCrimsonDark,
    onError = DarkTextPrimary,
    outline = DarkGlassBorder,
    outlineVariant = DarkGlassBorder
)

val CyberiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

object CyberiTheme {
    val colors: CyberiColors
        @Composable
        get() = LocalCyberiColors.current
}

@Composable
fun CyberiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberiDarkColorScheme else CyberiLightColorScheme
    val cyberiColors = if (darkTheme) DarkCyberiColors else LightCyberiColors

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalCyberiColors provides cyberiColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CyberiTypography,
            shapes = CyberiShapes,
            content = content
        )
    }
}
