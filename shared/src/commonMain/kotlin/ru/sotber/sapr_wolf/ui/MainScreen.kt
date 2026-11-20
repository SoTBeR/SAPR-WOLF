package ru.sotber.sapr_wolf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.postprocessor.ReportFormat
import ru.sotber.sapr_wolf.ui.components.CalculationErrorDialog
import ru.sotber.sapr_wolf.ui.components.ConstructionCanvas
import ru.sotber.sapr_wolf.ui.components.EpureDisplayMode
import ru.sotber.sapr_wolf.ui.components.ExportReportDialog
import ru.sotber.sapr_wolf.ui.components.mainAppBar.MainAppBar
import ru.sotber.sapr_wolf.ui.components.ProjectFileAlertDialog
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors

@Composable
fun MainScreen(
    onToggleTheme: () -> Unit = {},
    viewModel: MainViewModel = viewModel { MainViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        uiState = uiState,
        onSelectRod = viewModel::selectRod,
        onSelectNode = viewModel::selectNode,
        onClearSelection = viewModel::clearSelection,
        onAddRodLeft = viewModel::addRodLeft,
        onAddRodRight = viewModel::addRodRight,
        onInsertRodLeft = viewModel::insertRodLeft,
        onInsertRodRight = viewModel::insertRodRight,
        onDeleteRod = viewModel::deleteRod,
        onUpdateRod = viewModel::updateRod,
        onUpdateNodeForce = viewModel::updateNodeForce,
        onToggleFixedNode = viewModel::toggleFixedNode,
        onStartProject = viewModel::startNewProject,
        onOpenProject = viewModel::openProjectFromFile,
        onSaveProject = viewModel::saveProjectToFile,
        onSaveProjectAs = viewModel::saveProjectAsToFile,
        onDismissFileDialogAlert = viewModel::dismissFileDialogAlert,
        onDismissCalculationError = viewModel::dismissCalculationError,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onSelectStage = viewModel::selectStage,
        onCalculate = viewModel::calculate,
        onSelectEpureMode = viewModel::setEpureDisplayMode,
        onInspectSection = viewModel::inspectSection,
        onToggleTheme = onToggleTheme,
        onExportReport = viewModel::openExportReportDialog,
        onConfirmExportReport = viewModel::exportReport,
        onDismissExportReportDialog = viewModel::dismissExportReportDialog,
    )
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onSelectRod: (Int) -> Unit = {},
    onSelectNode: (Int) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onAddRodLeft: () -> Unit = {},
    onAddRodRight: () -> Unit = {},
    onInsertRodLeft: (Int) -> Unit = {},
    onInsertRodRight: (Int) -> Unit = {},
    onDeleteRod: (Int) -> Unit = {},
    onUpdateRod: (Int, Rod) -> Unit = { _, _ -> },
    onUpdateNodeForce: (Int, Double) -> Unit = { _, _ -> },
    onToggleFixedNode: (Int) -> Unit = {},
    onStartProject: (Boolean) -> Unit = {},
    onOpenProject: () -> Unit = {},
    onSaveProject: () -> Unit = {},
    onSaveProjectAs: () -> Unit = {},
    onDismissFileDialogAlert: () -> Unit = {},
    onDismissCalculationError: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onSelectStage: (AppStage) -> Unit = {},
    onCalculate: () -> Unit = {},
    onSelectEpureMode: (EpureDisplayMode) -> Unit = {},
    onInspectSection: (Int, Double) -> Unit = { _, _ -> },
    onToggleTheme: () -> Unit = {},
    onExportReport: () -> Unit = {},
    onConfirmExportReport: (ReportFormat, Boolean) -> Unit = { _, _ -> },
    onDismissExportReportDialog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val viewportState = remember { ViewportState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(uiState.projectLoadVersion) {
        if (uiState.projectLoadVersion > 0) {
            viewportState.resetTo1to1()
        }
    }

    val mainAppBarRowsHazeState = rememberHazeState()
    val extraColors = MaterialThemeExtraColors

    val processorResult = (uiState.calculationState as? CalculationState.Success)
        ?.takeIf { uiState.selectedStage == AppStage.Processor }
    val postprocessorResult = (uiState.calculationState as? CalculationState.Success)
        ?.takeIf { uiState.selectedStage == AppStage.Postprocessor }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.canvasBase)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { uiState.isFileDialogOpen }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Spacebar) {
                    val isShift = keyEvent.isShiftPressed

                    viewportState.resetTo1to1(horizontalOnly = isShift)

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Z && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    val isShift = keyEvent.isShiftPressed

                    if (isShift) {
                        onRedo()
                    } else {
                        onUndo()
                    }

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Y && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    onRedo()

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Q && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    onAddRodLeft()

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.E && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    onAddRodRight()

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.N && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    val isShift = keyEvent.isShiftPressed

                    onStartProject(isShift)

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.O && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    onOpenProject()

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.S && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    val isShift = keyEvent.isShiftPressed

                    if (isShift) {
                        onSaveProjectAs()
                    } else {
                        onSaveProject()
                    }

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.T && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    onToggleTheme()

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Delete) {
                    uiState.selectedRodIndex?.let {
                        onDeleteRod(it)
                    }

                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter && keyEvent.isCtrlPressed) {
                    onCalculate()

                    true
                } else {
                    false
                }
            }
    ) {
        ConstructionCanvas(
            modifier = Modifier
                .hazeSource(mainAppBarRowsHazeState)
                .background(extraColors.canvasBase),
            construction = uiState.construction,
            viewportState = viewportState,
            processorResult = processorResult,
            postprocessorResult = postprocessorResult,
            epureDisplayMode = uiState.epureDisplayMode,
            inspectedRodIndex = uiState.inspectedRodIndex,
            inspectedLocalX = uiState.inspectedLocalX,
            selectedRodIndex = uiState.selectedRodIndex,
            selectedNodeIndex = uiState.selectedNodeIndex,
            onSelectRod = { rodIdx ->
                focusRequester.requestFocus()
                onSelectRod(rodIdx)
            },
            onSelectNode = { nodeIdx ->
                focusRequester.requestFocus()
                onSelectNode(nodeIdx)
            },
            onClearSelection = {
                focusRequester.requestFocus()
                onClearSelection()
            },
            onAddRodLeft = {
                focusRequester.requestFocus()
                onAddRodLeft()
            },
            onAddRodRight = {
                focusRequester.requestFocus()
                onAddRodRight()
            },
            onInspectSection = if (postprocessorResult != null) {
                { rodIdx, localX ->
                    focusRequester.requestFocus()
                    onInspectSection(rodIdx, localX)
                }
            } else {
                null
            },
        )

        MainAppBar(
            rowsHazeState = mainAppBarRowsHazeState,
            construction = uiState.construction,
            projectName = uiState.projectName,
            selectedStage = uiState.selectedStage,
            isProcessorEnabled = uiState.isProcessorEnabled,
            isPostprocessorEnabled = uiState.isPostprocessorEnabled,
            epureDisplayMode = uiState.epureDisplayMode,
            inspectedRodIndex = uiState.inspectedRodIndex,
            inspectedLocalX = uiState.inspectedLocalX,
            zoomLabel = viewportState.zoomPercent,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            canCalculate = uiState.canCalculate,
            validationIssues = uiState.validationIssues,
            calculationState = uiState.calculationState,
            selectedRodIndex = uiState.selectedRodIndex,
            selectedNodeIndex = uiState.selectedNodeIndex,
            onSelectRod = { rodIdx ->
                focusRequester.requestFocus()
                onSelectRod(rodIdx)
            },
            onSelectNode = { nodeIdx ->
                focusRequester.requestFocus()
                onSelectNode(nodeIdx)
            },
            onClearSelection = {
                focusRequester.requestFocus()
                onClearSelection()
            },
            onUpdateRod = onUpdateRod,
            onInsertRodLeft = { rodIdx ->
                focusRequester.requestFocus()
                onInsertRodLeft(rodIdx)
            },
            onInsertRodRight = { rodIdx ->
                focusRequester.requestFocus()
                onInsertRodRight(rodIdx)
            },
            onDeleteRod = { rodIdx ->
                focusRequester.requestFocus()
                onDeleteRod(rodIdx)
            },
            onUpdateNodeForce = onUpdateNodeForce,
            onToggleFixedNode = { nodeIdx ->
                focusRequester.requestFocus()
                onToggleFixedNode(nodeIdx)
            },
            onStartProject = {
                focusRequester.requestFocus()
                onStartProject(false)
                viewportState.resetTo1to1()
            },
            onOpenProject = {
                focusRequester.requestFocus()
                onOpenProject()
            },
            onSaveProject = {
                focusRequester.requestFocus()
                onSaveProject()
            },
            onSaveProjectAs = {
                focusRequester.requestFocus()
                onSaveProjectAs()
            },
            onUndo = {
                focusRequester.requestFocus()
                onUndo()
            },
            onRedo = {
                focusRequester.requestFocus()
                onRedo()
            },
            onSelectStage = { stage ->
                focusRequester.requestFocus()
                onSelectStage(stage)
            },
            onCalculate = {
                focusRequester.requestFocus()
                onCalculate()
            },
            onSelectEpureMode = { mode ->
                focusRequester.requestFocus()
                onSelectEpureMode(mode)
            },
            onInspectSection = onInspectSection,
            onResetScale = {
                focusRequester.requestFocus()
                viewportState.resetTo1to1()
            },
            onToggleTheme = {
                focusRequester.requestFocus()
                onToggleTheme()
            },
            onExportReport = {
                focusRequester.requestFocus()
                onExportReport()
            },
            onOpenSettings = onOpenSettings,
        )

        if (uiState.isFileDialogOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.08f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }

        if (uiState.isExportReportDialogOpen) {
            ExportReportDialog(
                onSelectFormat = { format, openAfterExport ->
                    onConfirmExportReport(format, openAfterExport)
                    focusRequester.requestFocus()
                },
                onDismiss = {
                    onDismissExportReportDialog()
                    focusRequester.requestFocus()
                },
            )
        }

        val calculationError = uiState.calculationState as? CalculationState.Error
        if (calculationError != null) {
            CalculationErrorDialog(
                error = calculationError,
                onDismiss = {
                    onDismissCalculationError()
                    focusRequester.requestFocus()
                },
            )
        }

        val alert = uiState.fileDialogAlert
        if (alert != null) {
            ProjectFileAlertDialog(
                alert = alert,
                onDismiss = {
                    onDismissFileDialogAlert()
                    focusRequester.requestFocus()
                },
            )
        }
    }
}
