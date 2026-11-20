package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.ValidationIssue
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.ui.CalculationState
import ru.sotber.sapr_wolf.ui.components.EpureDisplayMode
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoTextStyle
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold
import sapr_wolf.shared.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun MainAppBar(
    rowsHazeState: HazeState,
    modifier: Modifier = Modifier,
    construction: Construction = Construction(),
    projectName: String? = null,
    selectedStage: AppStage = AppStage.Preprocessor,
    isProcessorEnabled: Boolean = true,
    isPostprocessorEnabled: Boolean = false,
    epureDisplayMode: EpureDisplayMode = EpureDisplayMode.All,
    inspectedRodIndex: Int? = null,
    inspectedLocalX: Double = 0.0,
    zoomLabel: Double = 100.0,
    canUndo: Boolean = true,
    canRedo: Boolean = true,
    canCalculate: Boolean = true,
    validationIssues: List<ValidationIssue> = emptyList(),
    calculationState: CalculationState = CalculationState.Idle,
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
    onSaveProjectAs: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onSelectStage: (AppStage) -> Unit = {},
    onCalculate: () -> Unit = {},
    onSelectEpureMode: (EpureDisplayMode) -> Unit = {},
    onInspectSection: (Int, Double) -> Unit = { _, _ -> },
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

    var leftRowWidthPx by remember { mutableIntStateOf(0) }
    var leftRowHeightPx by remember { mutableIntStateOf(0) }
    var rightRowWidthPx by remember { mutableIntStateOf(0) }
    var isFileMenuExpanded by remember { mutableStateOf(false) }

    val processorResult = (calculationState as? CalculationState.Success)
        ?.takeIf { selectedStage == AppStage.Processor }
    val postprocessorResult = (calculationState as? CalculationState.Success)
        ?.takeIf { selectedStage == AppStage.Postprocessor }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (processorResult != null && leftRowWidthPx > 0) {
            val resultsPanelWidth = with(density) { (leftRowWidthPx * 2).toDp() }

            MainAppBarProcessorNodesTable(
                hazeState = rowsHazeState,
                construction = construction,
                result = processorResult,
                selectedNodeIndex = selectedNodeIndex,
                onSelectNode = onSelectNode,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(MaterialThemeSpacing.floatingMargin)
                    .width(resultsPanelWidth),
            )
        }

        if (postprocessorResult != null && leftRowWidthPx > 0 && leftRowHeightPx > 0 && construction.rods.isNotEmpty()) {
            val leftPanelWidth = with(density) { leftRowWidthPx.toDp() }
            val leftRowHeight = with(density) { leftRowHeightPx.toDp() }
            val resultsPanelWidth = leftPanelWidth * 2

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .padding(
                        start = MaterialThemeSpacing.floatingMargin,
                        top = MaterialThemeSpacing.floatingMargin + leftRowHeight + MaterialThemeSpacing.medium,
                        end = MaterialThemeSpacing.floatingMargin,
                        bottom = MaterialThemeSpacing.floatingMargin,
                    ),
            ) {
                MainAppBarPostprocessorSectionPanel(
                    hazeState = rowsHazeState,
                    construction = construction,
                    result = postprocessorResult,
                    selectedRodIndex = selectedRodIndex,
                    inspectedRodIndex = inspectedRodIndex,
                    inspectedLocalX = inspectedLocalX,
                    onInspectSection = onInspectSection,
                    modifier = Modifier.width(leftPanelWidth),
                )

                MainAppBarPostprocessorResultsTable(
                    hazeState = rowsHazeState,
                    construction = construction,
                    result = postprocessorResult,
                    selectedRodIndex = selectedRodIndex,
                    inspectedRodIndex = inspectedRodIndex,
                    onInspectSection = onInspectSection,
                    modifier = Modifier
                        .width(resultsPanelWidth)
                        .weight(1f),
                )

                MainAppBarPostprocessorPlotsPanel(
                    hazeState = rowsHazeState,
                    construction = construction,
                    result = postprocessorResult,
                    selectedRodIndex = selectedRodIndex,
                    inspectedRodIndex = inspectedRodIndex,
                    inspectedLocalX = inspectedLocalX,
                    onInspectSection = onInspectSection,
                    modifier = Modifier
                        .width(resultsPanelWidth)
                        .weight(1f),
                )
            }
        }

        if (!projectName.isNullOrBlank()) {
            val maxNameWidth = if (rightRowWidthPx > 0) {
                with(density) { rightRowWidthPx.toDp() }
            } else {
                240.dp
            }

            MainAppBarRow(
                hazeState = rowsHazeState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MaterialThemeSpacing.floatingMargin),
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.labelMediumBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = maxNameWidth)
                        .padding(
                            horizontal = MaterialThemeSpacing.mediumSmall,
                            vertical = MaterialThemeSpacing.small,
                        ),
                )
            }
        }

        if (isFileMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isFileMenuExpanded = false
                        }
                    },
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialThemeSpacing.floatingMargin,
                    vertical = MaterialThemeSpacing.floatingMargin,
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = if (selectedStage == AppStage.Preprocessor && validationIssues.isNotEmpty()) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier
                },
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
                ) {
                    MainAppBarRow(
                        hazeState = rowsHazeState,
                        modifier = Modifier.onSizeChanged { size ->
                            leftRowWidthPx = size.width
                            leftRowHeightPx = size.height
                        },
                    ) {
                        MainAppBarButton(
                            iconRes = Res.drawable.ic_file_menu,
                            textRes = Res.string.file_menu,
                            isSelected = isFileMenuExpanded,
                            onClick = {
                                isFileMenuExpanded = !isFileMenuExpanded
                            },
                        )

                        MainAppBarButton(
                            iconRes = Res.drawable.ic_start_project,
                            textRes = Res.string.start_project,
                            onClick = {
                                isFileMenuExpanded = false
                                onStartProject()
                            },
                        )

                        MainAppBarRowDivider()

                        MainAppBarIconButton(
                            iconRes = Res.drawable.ic_undo,
                            enabled = canUndo,
                            onClick = {
                                isFileMenuExpanded = false
                                onUndo()
                            },
                        )

                        MainAppBarIconButton(
                            iconRes = Res.drawable.ic_redo,
                            enabled = canRedo,
                            onClick = {
                                isFileMenuExpanded = false
                                onRedo()
                            },
                        )
                    }

                    if (isFileMenuExpanded) {
                        MainAppBarFileMenu(
                            hazeState = rowsHazeState,
                            onOpenProject = {
                                isFileMenuExpanded = false
                                onOpenProject()
                            },
                            onSaveProject = {
                                isFileMenuExpanded = false
                                onSaveProject()
                            },
                            onSaveProjectAs = {
                                isFileMenuExpanded = false
                                onSaveProjectAs()
                            },
                        )
                    }
                }

                if (selectedStage == AppStage.Preprocessor && validationIssues.isNotEmpty() && leftRowWidthPx > 0) {
                    val leftPanelWidth = with(density) { leftRowWidthPx.toDp() }

                    MainAppBarValidationIssues(
                        hazeState = rowsHazeState,
                        issues = validationIssues,
                        modifier = Modifier.width(leftPanelWidth),
                    )
                }
            }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
        ) {
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

            if (postprocessorResult != null) {
                MainAppBarEpureSelector(
                    hazeState = rowsHazeState,
                    selectedMode = epureDisplayMode,
                    onSelectMode = onSelectEpureMode,
                )
            }
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
                    text = stringResource(Res.string.zoom_percent, zoomLabel.roundToInt()),
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
                    enabled = isPostprocessorEnabled,
                    onClick = onExportReport,
                )

                MainAppBarIconButton(
                    iconRes = Res.drawable.ic_info,
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
}

@Composable
private fun MainAppBarFileMenu(
    hazeState: HazeState,
    onOpenProject: () -> Unit,
    onSaveProject: () -> Unit,
    onSaveProjectAs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier.width(IntrinsicSize.Max),
    ) {
        MainAppBarButton(
            iconRes = Res.drawable.ic_open_project,
            textRes = Res.string.open_project,
            contentAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenProject,
        )

        MainAppBarButton(
            iconRes = Res.drawable.ic_save_project,
            textRes = Res.string.save_project,
            contentAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSaveProject,
        )

        MainAppBarButton(
            iconRes = Res.drawable.ic_save_as_project,
            textRes = Res.string.save_project_as,
            contentAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSaveProjectAs,
        )
    }
}
