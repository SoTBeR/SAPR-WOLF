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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.ui.components.ConstructionCanvas
import ru.sotber.sapr_wolf.ui.components.MainAppBar
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
        onStartProject = viewModel::startNewProject,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onSelectStage = viewModel::selectStage,
        onCalculate = viewModel::calculate,
        onToggleTheme = onToggleTheme,
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
    onStartProject: () -> Unit = {},
    onOpenProject: () -> Unit = {},
    onSaveProject: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onSelectStage: (AppStage) -> Unit = {},
    onCalculate: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onExportReport: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val viewportState = remember { ViewportState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val mainAppBarRowsHazeState = rememberHazeState()
    val extraColors = MaterialThemeExtraColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.canvasBase)
            .focusRequester(focusRequester)
            .focusable()
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
    ) {
        ConstructionCanvas(
            modifier = Modifier
                .hazeSource(mainAppBarRowsHazeState)
                .background(extraColors.canvasBase),
            construction = uiState.construction,
            viewportState = viewportState,
            selectedRodIndex = uiState.selectedRodIndex,
            selectedNodeIndex = uiState.selectedNodeIndex,
            onSelectRod = { rodIdx ->
                onSelectRod(rodIdx)
                focusRequester.requestFocus()
            },
            onSelectNode = { nodeIdx ->
                onSelectNode(nodeIdx)
                focusRequester.requestFocus()
            },
            onClearSelection = {
                onClearSelection()
                focusRequester.requestFocus()
            },
            onAddRodLeft = {
                onAddRodLeft()
                focusRequester.requestFocus()
            },
            onAddRodRight = {
                onAddRodRight()
                focusRequester.requestFocus()
            },
        )

        MainAppBar(
            rowsHazeState = mainAppBarRowsHazeState,
            selectedStage = uiState.selectedStage,
            isProcessorEnabled = uiState.isProcessorEnabled,
            isPostprocessorEnabled = uiState.isPostprocessorEnabled,
            zoomLabel = viewportState.zoomPercent,
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            canCalculate = uiState.canCalculate,
            onStartProject = {
                onStartProject()
                viewportState.resetTo1to1()
                focusRequester.requestFocus()
            },
            onOpenProject = onOpenProject,
            onSaveProject = onSaveProject,
            onUndo = {
                onUndo()
                focusRequester.requestFocus()
            },
            onRedo = {
                onRedo()
                focusRequester.requestFocus()
            },
            onSelectStage = { stage ->
                onSelectStage(stage)
                focusRequester.requestFocus()
            },
            onCalculate = {
                onCalculate()
                focusRequester.requestFocus()
            },
            onResetScale = {
                viewportState.resetTo1to1()
                focusRequester.requestFocus()
            },
            onToggleTheme = {
                onToggleTheme()
                focusRequester.requestFocus()
            },
            onExportReport = onExportReport,
            onOpenSettings = onOpenSettings,
        )
    }
}