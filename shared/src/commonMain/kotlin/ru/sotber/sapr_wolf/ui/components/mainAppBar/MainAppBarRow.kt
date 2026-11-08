package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import ru.sotber.sapr_wolf.ui.theme.CapsuleShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeBlurRadius
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing

@Composable
fun MainAppBarRow(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: Shape = CapsuleShape,
    content: @Composable RowScope.() -> Unit,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
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
                .padding(MaterialThemeSpacing.extraSmall),
            content = content,
        )
    }
}

@Composable
fun MainAppBarRowDivider(
    modifier: Modifier = Modifier,
) {
    VerticalDivider(
        modifier = modifier
            .padding(horizontal = MaterialThemeSpacing.extraSmall)
            .height(MaterialThemeSpacing.large),
        thickness = DividerDefaults.Thickness,
        color = MaterialTheme.colorScheme.outline,
    )
}
