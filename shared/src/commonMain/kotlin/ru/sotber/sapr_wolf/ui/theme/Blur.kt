package ru.sotber.sapr_wolf.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BlurRadius(
    val level0: Dp = 0.dp,
    val scrim: Dp = 6.dp,
    val chip: Dp = 12.dp,
    val panel: Dp = 24.dp,
    val modal: Dp = 40.dp,
)

val LocalBlurRadius = staticCompositionLocalOf { BlurRadius() }

val MaterialThemeBlurRadius: BlurRadius
    @Composable get() = LocalBlurRadius.current
