package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import dev.chrisbanes.haze.HazeState
import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoTextStyle
import sapr_wolf.shared.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun MainAppBar(
    rowsHazeState: HazeState,
    modifier: Modifier = Modifier,
    construction: Construction = Construction(),
    selectedStage: AppStage = AppStage.Preprocessor,
    isProcessorEnabled: Boolean = true,
    isPostprocessorEnabled: Boolean = false,
    zoomLabel: Double = 100.0,
    canUndo: Boolean = true,
    canRedo: Boolean = true,
    canCalculate: Boolean = true,
    selectedRodIndex: Int? = null,
    selectedNodeIndex: Int? = null,
    onSelectRod: (Int) -> Unit = {},
    onSelectNode: (Int) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onUpdateRod: (Int, Rod) -> Unit = { _, _ -> },
    onInsertRodLeft: (Int) -> Unit = {},
    onInsertRodRight: (Int) -> Unit = {},
    onDeleteRod: (Int) -> Unit = {},
    onUpdateNodeForce: (Int, Double) -> Unit = { _, _ -> },
    onToggleFixedNode: (Int) -> Unit = {},
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
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()
    val minZoomTextWidthPx = remember(textMeasurer) {
        textMeasurer.measure(
            text = "_".repeat(4),
            style = MonoTextStyle,
        ).size.width
    }

    var rightRowWidthPx by remember { mutableIntStateOf(0) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
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
            MainAppBarButton(
                iconRes = Res.drawable.ic_start_project,
                textRes = Res.string.start_project,
                onClick = onStartProject,
            )

            MainAppBarButton(
                iconRes = Res.drawable.ic_open_project,
                textRes = Res.string.open_project,
                onClick = onOpenProject,
            )

            MainAppBarButton(
                iconRes = Res.drawable.ic_save_project,
                textRes = Res.string.save_project,
                onClick = onSaveProject,
            )

            MainAppBarRowDivider()

            MainAppBarIconButton(
                iconRes = Res.drawable.ic_undo,
                enabled = canUndo,
                onClick = onUndo,
            )

            MainAppBarIconButton(
                iconRes = Res.drawable.ic_redo,
                enabled = canRedo,
                onClick = onRedo,
            )
        }

        MainAppBarRow(
            hazeState = rowsHazeState,
        ) {
            MainAppBarButton(
                textRes = Res.string.preprocessor,
                style = MainAppBarButtonStyle.Segment,
                isSelected = selectedStage == AppStage.Preprocessor,
                onClick = { onSelectStage(AppStage.Preprocessor) },
            )

            MainAppBarButton(
                textRes = Res.string.processor,
                style = MainAppBarButtonStyle.Segment,
                enabled = isProcessorEnabled,
                isSelected = selectedStage == AppStage.Processor,
                onClick = { onSelectStage(AppStage.Processor) },
            )

            MainAppBarButton(
                textRes = Res.string.postprocessor,
                style = MainAppBarButtonStyle.Segment,
                enabled = isPostprocessorEnabled,
                isSelected = selectedStage == AppStage.Postprocessor,
                onClick = { onSelectStage(AppStage.Postprocessor) },
            )

            MainAppBarRowDivider()

            MainAppBarButton(
                textRes = Res.string.calculate,
                style = MainAppBarButtonStyle.Primary,
                enabled = canCalculate,
                onClick = onCalculate,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
        ) {
            MainAppBarRow(
                hazeState = rowsHazeState,
                modifier = Modifier.onSizeChanged { size ->
                    rightRowWidthPx = size.width
                },
            ) {
                MainAppBarIconButton(
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

                MainAppBarIconButton(
                    iconRes = if (isDarkTheme) Res.drawable.ic_dark_mode else Res.drawable.ic_light_mode,
                    onClick = onToggleTheme,
                )

                MainAppBarIconButton(
                    iconRes = Res.drawable.ic_export_report,
                    onClick = onExportReport,
                )

                MainAppBarIconButton(
                    iconRes = Res.drawable.ic_settings,
                    onClick = onOpenSettings,
                )
            }

            if (selectedStage == AppStage.Preprocessor && rightRowWidthPx > 0 && construction.rods.isNotEmpty()) {
                val panelWidth = with(density) { rightRowWidthPx.toDp() }

                when {
                    selectedRodIndex != null && selectedRodIndex in construction.rods.indices -> {
                        val rod = construction.rods[selectedRodIndex]

                        MainAppBarRodInspector(
                            hazeState = rowsHazeState,
                            rodIndex = selectedRodIndex,
                            rodsCount = construction.rods.size,
                            rod = rod,
                            onSelectRod = onSelectRod,
                            onSelectNodeTab = {
                                onSelectNode(selectedRodIndex)
                            },
                            onUpdateRod = { updatedRod ->
                                onUpdateRod(selectedRodIndex, updatedRod)
                            },
                            onInsertRodLeft = {
                                onInsertRodLeft(selectedRodIndex)
                            },
                            onInsertRodRight = {
                                onInsertRodRight(selectedRodIndex)
                            },
                            onDeleteRod = {
                                onDeleteRod(selectedRodIndex)
                            },
                            onClose = onClearSelection,
                            modifier = Modifier.width(panelWidth),
                        )
                    }

                    selectedNodeIndex != null && selectedNodeIndex in 0 until construction.nodeCount -> {
                        val nodeForce = construction.nodeForces.getOrElse(selectedNodeIndex) { 0.0 }
                        val isNodeFixed = selectedNodeIndex in construction.fixedNodes

                        MainAppBarNodeInspector(
                            hazeState = rowsHazeState,
                            nodeIndex = selectedNodeIndex,
                            nodesCount = construction.nodeCount,
                            nodeForce = nodeForce,
                            isNodeFixed = isNodeFixed,
                            onSelectNode = onSelectNode,
                            onSelectRodTab = {
                                val targetRodIndex = selectedNodeIndex.coerceIn(0, construction.rods.lastIndex)
                                onSelectRod(targetRodIndex)
                            },
                            onUpdateNodeForce = { updatedForce ->
                                onUpdateNodeForce(selectedNodeIndex, updatedForce)
                            },
                            onToggleNodeFixed = {
                                onToggleFixedNode(selectedNodeIndex)
                            },
                            onClose = onClearSelection,
                            modifier = Modifier.width(panelWidth),
                        )
                    }
                }
            }
        }
    }
}
