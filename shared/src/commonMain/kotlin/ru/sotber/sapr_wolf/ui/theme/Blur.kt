package ru.sotber.sapr_wolf.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BlurRadius(
    val level0: Dp = 0.dp,
    val level1: Dp = 6.dp,
    val level2: Dp = 12.dp,
    val level3: Dp = 24.dp,
    val level4: Dp = 40.dp,
)

val LocalBlurRadius = staticCompositionLocalOf { BlurRadius() }

val MaterialThemeBlurRadius: BlurRadius
    @Composable get() = LocalBlurRadius.current
