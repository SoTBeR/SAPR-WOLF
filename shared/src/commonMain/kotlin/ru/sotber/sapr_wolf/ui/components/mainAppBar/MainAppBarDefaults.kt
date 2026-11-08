package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MainAppBarDefaults {
    @Composable
    fun containerColor(
        style: MainAppBarButtonStyle,
        enabled: Boolean,
        isSelected: Boolean,
        isHovered: Boolean,
    ): Color {
        val colorScheme = MaterialTheme.colorScheme

        if (!enabled) {
            return when (style) {
                MainAppBarButtonStyle.Primary,
                MainAppBarButtonStyle.Tonal,
                MainAppBarButtonStyle.DestructiveTonal -> colorScheme.surfaceVariant
                MainAppBarButtonStyle.Ghost,
                MainAppBarButtonStyle.Segment -> Color.Transparent
            }
        }

        return when (style) {
            MainAppBarButtonStyle.Ghost -> when {
                isSelected -> colorScheme.primaryContainer
                isHovered -> colorScheme.surfaceVariant
                else -> Color.Transparent
            }

            MainAppBarButtonStyle.Segment -> when {
                isSelected -> colorScheme.primaryContainer
                isHovered -> colorScheme.surfaceVariant
                else -> Color.Transparent
            }

            MainAppBarButtonStyle.Tonal -> when {
                isSelected || isHovered -> colorScheme.primaryContainer
                else -> colorScheme.surfaceVariant
            }

            MainAppBarButtonStyle.Primary -> colorScheme.primary

            MainAppBarButtonStyle.DestructiveTonal -> when {
                isSelected || isHovered -> colorScheme.error.copy(alpha = 0.22f)
                else -> colorScheme.error.copy(alpha = 0.12f)
            }
        }
    }

    @Composable
    fun contentColor(
        style: MainAppBarButtonStyle,
        enabled: Boolean,
        isSelected: Boolean,
        isHovered: Boolean,
    ): Color {
        val colorScheme = MaterialTheme.colorScheme

        if (!enabled) {
            return colorScheme.outlineVariant
        }

        return when (style) {
            MainAppBarButtonStyle.Ghost -> when {
                isSelected -> colorScheme.onPrimaryContainer
                else -> colorScheme.onSurface
            }

            MainAppBarButtonStyle.Segment -> when {
                isSelected -> colorScheme.onPrimaryContainer
                isHovered -> colorScheme.onSurface
                else -> colorScheme.onSurfaceVariant
            }

            MainAppBarButtonStyle.Tonal -> when {
                isSelected || isHovered -> colorScheme.onPrimaryContainer
                else -> colorScheme.onSurface
            }

            MainAppBarButtonStyle.Primary -> colorScheme.onPrimary

            MainAppBarButtonStyle.DestructiveTonal -> colorScheme.error
        }
    }
}
