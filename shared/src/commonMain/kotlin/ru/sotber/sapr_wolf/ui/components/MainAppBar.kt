package ru.sotber.sapr_wolf.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.ui.theme.*
import sapr_wolf.shared.generated.resources.*

enum class MainAppBarButtonStyle {
    Ghost,
    Segment,
    Tonal,
    Primary,
}

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
                MainAppBarButtonStyle.Tonal -> colorScheme.surfaceVariant
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
        }
    }
}

@Composable
fun MainAppBar(
    rowsHazeState: HazeState,
    modifier: Modifier = Modifier,
    selectedStage: AppStage = AppStage.Preprocessor,
    isProcessorEnabled: Boolean = true,
    isPostprocessorEnabled: Boolean = false,
    zoomLabel: Double = 100.0,
    canUndo: Boolean = true,
    canRedo: Boolean = true,
    canCalculate: Boolean = true,
    onStartProject: () -> Unit = {},
    onOpenProject: () -> Unit = {},
    onSaveProject: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onSelectStage: (AppStage) -> Unit = {},
    onCalculate: () -> Unit = {},
    onResetScale: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onExportReport: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val isDarkTheme = MaterialThemeExtraColors.isDark

    val textMeasurer = rememberTextMeasurer()
    val minZoomTextWidthPx = remember(textMeasurer) {
        textMeasurer.measure(
            text = "_".repeat(4),
            style = MonoTextStyle,
        ).size.width
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialThemeSpacing.floatingMargin,
                vertical = MaterialThemeSpacing.floatingMargin,
            ),
    ) {
        MainAppBarRow(
            hazeState = rowsHazeState,
        ) {
            MainAppBarRowButton(
                iconRes = Res.drawable.ic_start_project,
                textRes = Res.string.start_project,
                onClick = onStartProject,
            )

            MainAppBarRowButton(
                iconRes = Res.drawable.ic_open_project,
                textRes = Res.string.open_project,
                onClick = onOpenProject,
            )

            MainAppBarRowButton(
                iconRes = Res.drawable.ic_save_project,
                textRes = Res.string.save_project,
                onClick = onSaveProject,
            )

            MainAppBarRowDivider()

            MainAppBarRowIconButton(
                iconRes = Res.drawable.ic_undo,
                enabled = canUndo,
                onClick = onUndo,
            )

            MainAppBarRowIconButton(
                iconRes = Res.drawable.ic_redo,
                enabled = canRedo,
                onClick = onRedo,
            )
        }

        MainAppBarRow(
            hazeState = rowsHazeState,
        ) {
            MainAppBarRowButton(
                iconRes = Res.drawable.ic_preprocessor,
                textRes = Res.string.preprocessor,
                style = MainAppBarButtonStyle.Segment,
                isSelected = selectedStage == AppStage.Preprocessor,
                onClick = { onSelectStage(AppStage.Preprocessor) },
            )

            MainAppBarRowButton(
                iconRes = Res.drawable.ic_processor,
                textRes = Res.string.processor,
                style = MainAppBarButtonStyle.Segment,
                enabled = isProcessorEnabled,
                isSelected = selectedStage == AppStage.Processor,
                onClick = { onSelectStage(AppStage.Processor) },
            )

            MainAppBarRowButton(
                iconRes = Res.drawable.ic_postprocessor,
                textRes = Res.string.postprocessor,
                style = MainAppBarButtonStyle.Segment,
                enabled = isPostprocessorEnabled,
                isSelected = selectedStage == AppStage.Postprocessor,
                onClick = { onSelectStage(AppStage.Postprocessor) },
            )

            MainAppBarRowDivider()

            MainAppBarRowButton(
                textRes = Res.string.calculate,
                style = MainAppBarButtonStyle.Primary,
                enabled = canCalculate,
                onClick = onCalculate,
            )
        }

        MainAppBarRow(
            hazeState = rowsHazeState,
        ) {
            MainAppBarRowIconButton(
                iconRes = Res.drawable.ic_scale,
                onClick = onResetScale,
            )

            Text(
                text = "${zoomLabel.roundToInt()}%",
                style = MonoTextStyle,
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .padding(
                        end = MaterialThemeSpacing.small,
                    )
                    .layout { measurable, constraints ->
                        val resolvedMinWidth = minZoomTextWidthPx.coerceIn(
                            constraints.minWidth,
                            constraints.maxWidth,
                        )

                        val placeable = measurable.measure(
                            constraints.copy(minWidth = resolvedMinWidth)
                        )

                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    },
            )

            MainAppBarRowDivider()

            MainAppBarRowIconButton(
                iconRes = if (isDarkTheme) Res.drawable.ic_dark_mode else Res.drawable.ic_light_mode,
                onClick = onToggleTheme,
            )

            MainAppBarRowIconButton(
                iconRes = Res.drawable.ic_export_report,
                onClick = onExportReport,
            )

            MainAppBarRowIconButton(
                iconRes = Res.drawable.ic_settings,
                onClick = onOpenSettings,
            )
        }
    }
}

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

@Composable
fun MainAppBarRowButton(
    textRes: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: DrawableResource? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    style: MainAppBarButtonStyle = MainAppBarButtonStyle.Ghost,
    shape: Shape = CapsuleShape,
    textStyle: TextStyle? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor by animateColorAsState(
        targetValue = MainAppBarDefaults.containerColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )
    val contentColor by animateColorAsState(
        targetValue = MainAppBarDefaults.contentColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )

    val resolvedTextStyle = textStyle ?: if (isSelected || style == MainAppBarButtonStyle.Primary) {
        MaterialTheme.typography.labelMediumBold
    } else {
        MaterialTheme.typography.labelMedium
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = MaterialThemeSpacing.small,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = MaterialThemeSpacing.mediumSmall,
                vertical = MaterialThemeSpacing.small,
            ),
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = stringResource(textRes),
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }

            Text(
                text = stringResource(textRes),
                style = resolvedTextStyle,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MainAppBarRowIconButton(
    iconRes: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    style: MainAppBarButtonStyle = MainAppBarButtonStyle.Ghost,
    shape: Shape = CapsuleShape,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val containerColor by animateColorAsState(
        targetValue = MainAppBarDefaults.containerColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )
    val contentColor by animateColorAsState(
        targetValue = MainAppBarDefaults.contentColor(
            style = style,
            enabled = enabled,
            isSelected = isSelected,
            isHovered = isHovered,
        ),
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(MaterialThemeSpacing.small),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        }
    }
}
