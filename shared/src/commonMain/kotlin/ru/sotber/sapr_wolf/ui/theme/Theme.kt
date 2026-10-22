package ru.sotber.sapr_wolf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    SYSTEM,
    DARK,
    LIGHT,
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,

    error = ErrorDark,
    onError = OnErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,

    error = ErrorLight,
    onError = OnErrorLight,
)

data class SaprExtraColors(
    val isDark: Boolean,
    val canvasBase: Color,
    val canvasDots: Color,
    val tensionLine: Color,
    val compressionLine: Color,
    val supportHatch: Color,
    val glassDialog: Color,
    val glassChip: Color,
    val glassSpecular: Color,
)

val DarkSaprExtraColors = SaprExtraColors(
    isDark = true,
    canvasBase = CanvasBaseDark,
    canvasDots = CanvasDotsDark,
    tensionLine = TensionLineDark,
    compressionLine = CompressionLineDark,
    supportHatch = SupportHatchDark,
    glassDialog = GlassDialogDark,
    glassChip = GlassChipDark,
    glassSpecular = GlassSpecularDark,
)

val LightSaprExtraColors = SaprExtraColors(
    isDark = false,
    canvasBase = CanvasBaseLight,
    canvasDots = CanvasDotsLight,
    tensionLine = TensionLineLight,
    compressionLine = CompressionLineLight,
    supportHatch = SupportHatchLight,
    glassDialog = GlassDialogLight,
    glassChip = GlassChipLight,
    glassSpecular = GlassSpecularLight,
)

val LocalSaprExtraColors = staticCompositionLocalOf { DarkSaprExtraColors }

val MaterialThemeExtraColors: SaprExtraColors
    @Composable get() = LocalSaprExtraColors.current

@Composable
fun AppTheme.getColorScheme(darkTheme: Boolean = isSystemInDarkTheme()): ColorScheme {
    return when (this) {
        AppTheme.SYSTEM -> if (darkTheme) DarkColorScheme else LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
    }
}

@Composable
fun AppTheme.getExtraColors(darkTheme: Boolean = isSystemInDarkTheme()): SaprExtraColors {
    return when (this) {
        AppTheme.SYSTEM -> if (darkTheme) DarkSaprExtraColors else LightSaprExtraColors
        AppTheme.DARK -> DarkSaprExtraColors
        AppTheme.LIGHT -> LightSaprExtraColors
    }
}

@Composable
fun AppTheme(
    appTheme: AppTheme = AppTheme.DARK,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = appTheme.getColorScheme(darkTheme)
    val extraColors = appTheme.getExtraColors(darkTheme)

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalBlurRadius provides BlurRadius(),
        LocalSaprExtraColors provides extraColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
