package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeBlurRadius
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing

@Composable
fun MainAppBarColumn(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val canvasBaseColor = MaterialThemeExtraColors.canvasBase
    val panelBlurRadius = MaterialThemeBlurRadius.level3

    val blurStyle = remember(surfaceColor, canvasBaseColor, panelBlurRadius) {
        HazeBlurStyle {
            blurRadius(panelBlurRadius)
            noiseFactor(0f)
            backgroundColor(canvasBaseColor)
            colorEffects(listOf(HazeColorEffect.tint(surfaceColor)))
        }
    }

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = modifier
                .clip(shape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeBlur(
                            input = HazeInput.Backdrop(hazeState),
                            style = blurStyle,
                        )
                    } else {
                        Modifier.background(
                            color = surfaceColor,
                            shape = shape,
                        )
                    }
                )
                .border(
                    width = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline,
                    shape = shape,
                )
                .padding(MaterialThemeSpacing.small),
            content = content,
        )
    }
}
