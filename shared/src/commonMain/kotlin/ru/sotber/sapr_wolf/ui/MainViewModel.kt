package ru.sotber.sapr_wolf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.postprocessor.ReportFormat
import ru.sotber.sapr_wolf.postprocessor.analyzeStrength
import ru.sotber.sapr_wolf.postprocessor.buildReport
import ru.sotber.sapr_wolf.postprocessor.calculateEquilibriumResidual
import ru.sotber.sapr_wolf.postprocessor.calculateReactions
import ru.sotber.sapr_wolf.preprocessor.*
import ru.sotber.sapr_wolf.processor.calculateDisplacements
import ru.sotber.sapr_wolf.ui.components.EpureDisplayMode
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.dialog_default_project_filename

class MainViewModel(
    private val fileGateway: ProjectFileGateway = ProjectFileDialog,
) : ViewModel() {
    private val undoStack = ArrayDeque<Construction>()
    private val redoStack = ArrayDeque<Construction>()
    private var currentProjectFile: ProjectFileHandle? = null

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectRod(rodIndex: Int) {
        val current = _uiState.value

        if (rodIndex !in current.construction.rods.indices) return
        val rodLength = current.construction.rods[rodIndex].length

        _uiState.update {
            it.copy(
                selectedRodIndex = rodIndex,
                selectedNodeIndex = null,
                inspectedRodIndex = rodIndex,
                inspectedLocalX = if (it.inspectedRodIndex == rodIndex) {
                    it.inspectedLocalX.coerceIn(0.0, rodLength)
                } else {
                    0.0
                },
            )
        }
    }

    fun selectNode(nodeIndex: Int) {
        val current = _uiState.value
        if (current.construction.rods.isEmpty() || nodeIndex !in 0 until current.construction.nodeCount) return

        _uiState.update {
            it.copy(
                selectedNodeIndex = nodeIndex,
                selectedRodIndex = null,
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedRodIndex = null,
                selectedNodeIndex = null,
            )
        }
    }

    fun selectStage(stage: AppStage) {
        val current = _uiState.value
        when (stage) {
            AppStage.Preprocessor -> {
                _uiState.update { it.copy(selectedStage = AppStage.Preprocessor) }
            }

            AppStage.Processor -> {
                if (current.isCalculated) {
                    _uiState.update { it.copy(selectedStage = AppStage.Processor) }
                } else if (current.canCalculate) {
                    performCalculation(targetStage = AppStage.Processor)
                }
            }

            AppStage.Postprocessor -> {
                if (current.isCalculated) {
                    _uiState.update { it.copy(selectedStage = AppStage.Postprocessor) }
                }
            }
        }
    }

    fun setEpureDisplayMode(mode: EpureDisplayMode) {
        _uiState.update { it.copy(epureDisplayMode = mode) }
    }

    fun inspectSection(rodIndex: Int, localX: Double) {
        val current = _uiState.value
        if (rodIndex !in current.construction.rods.indices) return
        val clampedX = localX.coerceIn(0.0, current.construction.rods[rodIndex].length)

        _uiState.update {
            it.copy(
                selectedRodIndex = rodIndex,
                selectedNodeIndex = null,
                inspectedRodIndex = rodIndex,
                inspectedLocalX = clampedX,
            )
        }
    }

    fun openExportReportDialog() {
        val current = _uiState.value
        if (!current.isCalculated || current.isFileDialogOpen) return
        _uiState.update { it.copy(isExportReportDialogOpen = true) }
    }

    fun dismissExportReportDialog() {
        _uiState.update { it.copy(isExportReportDialogOpen = false) }
    }

    fun exportReport(
        format: ReportFormat,
        openAfterExport: Boolean = false,
    ) {
        val current = _uiState.value
        val result = current.calculationState as? CalculationState.Success ?: return
        if (current.isFileDialogOpen) return

        _uiState.update { it.copy(isExportReportDialogOpen = false) }

        viewModelScope.launch {
            withFileDialogLock {
                val reportContent = buildReport(
                    construction = current.construction,
                    displacements = result.displacements.toDoubleArray(),
                    projectName = current.projectName,
                    format = format,
                )
                val defaultFileName = current.projectName
                    ?: runCatching { getString(Res.string.dialog_default_project_filename) }
                        .getOrDefault("project")

                val saveResult = fileGateway.saveReportFile(
                    defaultFileName = defaultFileName,
                    extension = format.extension,
                    content = reportContent,
                    openAfterExport = openAfterExport,
                ) ?: return@launch

                saveResult.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            fileDialogAlert = FileDialogAlert.SaveError(details = error.message),
                        )
                    }
                }
            }
        }
    }

    fun calculate() {
        if (!_uiState.value.canCalculate) return

        performCalculation(targetStage = AppStage.Postprocessor)
    }

    fun addRodLeft() {
        val construction = _uiState.value.construction
        val firstRod = construction.rods.firstOrNull()
        val newRod = firstRod?.copy(distributedLoad = 0.0) ?: Rod.defaultRod()

        if (construction.rods.isEmpty()) {
            modifyConstruction(
                newConstruction = Construction(
                    rods = listOf(newRod),
                    nodeForces = listOf(0.0, 0.0),
                    fixedNodes = emptySet(),
                ),
            )

            return
        }

        val normalizedForces = List(construction.nodeCount) { idx ->
            construction.nodeForces.getOrElse(idx) { 0.0 }
        }
        val wasLeftEdgeFixed = 0 in construction.fixedNodes
        val newFixedNodes = if (wasLeftEdgeFixed) {
            (construction.fixedNodes - 0).map { it + 1 }.toSet() + 0
        } else {
            construction.fixedNodes.map { it + 1 }.toSet()
        }

        modifyConstruction(
            newConstruction = construction.copy(
                rods = listOf(newRod) + construction.rods,
                nodeForces = listOf(0.0) + normalizedForces,
                fixedNodes = newFixedNodes,
            ),
            transformSelectedRod = { rodIdx -> rodIdx?.let { it + 1 } },
            transformSelectedNode = { nodeIdx -> nodeIdx?.let { it + 1 } },
        )
    }

    fun addRodRight() {
        val construction = _uiState.value.construction
        val lastRod = construction.rods.lastOrNull()
        val newRod = lastRod?.copy(distributedLoad = 0.0) ?: Rod.defaultRod()

        if (construction.rods.isEmpty()) {
            modifyConstruction(
                newConstruction = Construction(
                    rods = listOf(newRod),
                    nodeForces = listOf(0.0, 0.0),
                    fixedNodes = emptySet(),
                ),
            )

            return
        }

        val normalizedForces = List(construction.nodeCount) { idx ->
            construction.nodeForces.getOrElse(idx) { 0.0 }
        }
        val oldLastNodeIdx = construction.rods.size
        val wasRightEdgeFixed = oldLastNodeIdx in construction.fixedNodes
        val newLastNodeIdx = oldLastNodeIdx + 1
        val newFixedNodes = if (wasRightEdgeFixed) {
            (construction.fixedNodes - oldLastNodeIdx) + newLastNodeIdx
        } else {
            construction.fixedNodes
        }

        modifyConstruction(
            newConstruction = construction.copy(
                rods = construction.rods + newRod,
                nodeForces = normalizedForces + 0.0,
                fixedNodes = newFixedNodes,
            ),
        )
    }

    fun insertRodLeft(rodIndex: Int) {
        val construction = _uiState.value.construction
        if (rodIndex !in construction.rods.indices) return

        if (rodIndex == 0) {
            addRodLeft()
            return
        }

        val newRod = construction.rods[rodIndex].copy(distributedLoad = 0.0)
        val updatedRods = construction.rods.toMutableList().apply {
            add(rodIndex, newRod)
        }
        val updatedForces = MutableList(construction.nodeCount) { idx ->
            construction.nodeForces.getOrElse(idx) { 0.0 }
        }.apply {
            add(rodIndex, 0.0)
        }
        val updatedFixedNodes = construction.fixedNodes.map { nodeIdx ->
            if (nodeIdx > rodIndex) nodeIdx + 1 else nodeIdx
        }.toSet()

        modifyConstruction(
            newConstruction = construction.copy(
                rods = updatedRods,
                nodeForces = updatedForces,
                fixedNodes = updatedFixedNodes,
            ),
            transformSelectedRod = { rodIndex + 1 },
        )
    }

    fun insertRodRight(rodIndex: Int) {
        val construction = _uiState.value.construction
        if (rodIndex !in construction.rods.indices) return

        if (rodIndex == construction.rods.lastIndex) {
            addRodRight()
            return
        }

        val insertIdx = rodIndex + 1
        val newRod = construction.rods[rodIndex].copy(distributedLoad = 0.0)
        val updatedRods = construction.rods.toMutableList().apply {
            add(insertIdx, newRod)
        }
        val updatedForces = MutableList(construction.nodeCount) { idx ->
            construction.nodeForces.getOrElse(idx) { 0.0 }
        }.apply {
            add(insertIdx, 0.0)
        }
        val updatedFixedNodes = construction.fixedNodes.map { nodeIdx ->
            if (nodeIdx >= insertIdx) nodeIdx + 1 else nodeIdx
        }.toSet()

        modifyConstruction(
            newConstruction = construction.copy(
                rods = updatedRods,
                nodeForces = updatedForces,
                fixedNodes = updatedFixedNodes,
            ),
            transformSelectedRod = { rodIndex },
        )
    }

    fun deleteRod(rodIndex: Int) {
        val construction = _uiState.value.construction
        if (rodIndex !in construction.rods.indices) return

        if (construction.rods.size == 1) {
            modifyConstruction(
                newConstruction = Construction(),
                transformSelectedRod = { null },
                transformSelectedNode = { null },
            )
            return
        }

        val updatedRods = construction.rods.filterIndexed { idx, _ -> idx != rodIndex }
        val normalizedForces = List(construction.nodeCount) { idx ->
            construction.nodeForces.getOrElse(idx) { 0.0 }
        }
        val removedNodeIndex = if (rodIndex == 0) 0 else rodIndex + 1
        val updatedForces = normalizedForces.filterIndexed { idx, _ -> idx != removedNodeIndex }

        val oldLastNodeIdx = construction.rods.size
        val newLastNodeIdx = updatedRods.size
        val updatedFixedNodes = buildSet {
            for (fixedIdx in construction.fixedNodes) {
                when {
                    rodIndex == 0 && fixedIdx == 0 -> add(0)
                    rodIndex == construction.rods.lastIndex && fixedIdx == oldLastNodeIdx -> add(newLastNodeIdx)
                    fixedIdx == removedNodeIndex -> Unit
                    fixedIdx > removedNodeIndex -> add(fixedIdx - 1)
                    else -> add(fixedIdx)
                }
            }
        }
        val nextSelectedRod = rodIndex.coerceAtMost(updatedRods.lastIndex)

        modifyConstruction(
            newConstruction = construction.copy(
                rods = updatedRods,
                nodeForces = updatedForces,
                fixedNodes = updatedFixedNodes,
            ),
            transformSelectedRod = { nextSelectedRod },
            transformSelectedNode = { null },
        )
    }

    fun updateRod(rodIndex: Int, rod: Rod) {
        val construction = _uiState.value.construction
        if (rodIndex !in construction.rods.indices) return

        val updatedRods = construction.rods.toMutableList().apply {
            this[rodIndex] = rod
        }

        modifyConstruction(
            newConstruction = construction.copy(rods = updatedRods),
        )
    }

    fun updateNodeForce(nodeIndex: Int, force: Double) {
        val construction = _uiState.value.construction

        if (construction.rods.isEmpty() || nodeIndex !in 0 until construction.nodeCount) return

        val updatedForces = MutableList(construction.nodeCount) { idx ->
            construction.nodeForces.getOrElse(idx) { 0.0 }
        }.apply {
            this[nodeIndex] = force
        }

        modifyConstruction(
            newConstruction = construction.copy(nodeForces = updatedForces),
        )
    }

    fun toggleFixedNode(nodeIndex: Int) {
        val construction = _uiState.value.construction

        if (construction.rods.isEmpty() || nodeIndex !in 0 until construction.nodeCount) return

        val updatedFixedNodes = if (nodeIndex in construction.fixedNodes) {
            construction.fixedNodes - nodeIndex
        } else {
            construction.fixedNodes + nodeIndex
        }

        modifyConstruction(
            newConstruction = construction.copy(fixedNodes = updatedFixedNodes),
        )
    }

    fun startNewProject(defaultConstruction: Boolean = false) {
        currentProjectFile = null

        val targetConstruction = if (defaultConstruction) {
            Construction.defaultDemoConstruction()
        } else {
            Construction()
        }

        if (targetConstruction == _uiState.value.construction) {
            _uiState.update {
                it.copy(
                    projectName = null,
                    selectedRodIndex = null,
                    selectedNodeIndex = null,
                )
            }
            return
        }

        modifyConstruction(
            newConstruction = targetConstruction,
            projectName = null,
            transformSelectedRod = { null },
            transformSelectedNode = { null },
        )
    }

    fun openProjectFromFile() {
        if (_uiState.value.isFileDialogOpen) return
        viewModelScope.launch {
            executeOpenProjectFromFile()
        }
    }

    fun saveProjectToFile() {
        if (_uiState.value.isFileDialogOpen) return
        viewModelScope.launch {
            executeSaveProjectToFile()
        }
    }

    fun saveProjectAsToFile() {
        if (_uiState.value.isFileDialogOpen) return
        viewModelScope.launch {
            executeSaveProjectAsToFile()
        }
    }

    internal suspend fun executeOpenProjectFromFile() {
        withFileDialogLock {
            val result = fileGateway.pickJsonFileToOpen() ?: return

            result
                .onSuccess { openedFile ->
                    loadProjectFromJson(
                        jsonString = openedFile.content,
                        projectName = openedFile.projectName,
                        projectFile = openedFile.file,
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            fileDialogAlert = FileDialogAlert.LoadError(details = error.message),
                        )
                    }
                }
        }
    }

    internal suspend fun executeSaveProjectToFile() {
        if (_uiState.value.isFileDialogOpen) return

        val activeFile = currentProjectFile
        if (activeFile == null) {
            executeSaveProjectAsToFile()
            return
        }

        val jsonContent = exportProjectToJson()

        fileGateway.overwriteJsonFile(
            file = activeFile,
            content = jsonContent,
        ).onFailure { error ->
            _uiState.update {
                it.copy(
                    fileDialogAlert = FileDialogAlert.SaveError(details = error.message),
                )
            }
        }
    }

    internal suspend fun executeSaveProjectAsToFile() {
        withFileDialogLock {
            val defaultFileName = _uiState.value.projectName
                ?: runCatching { getString(Res.string.dialog_default_project_filename) }
                    .getOrDefault("project.json")
            val jsonContent = exportProjectToJson()

            val result = fileGateway.saveJsonFileAs(
                defaultFileName = defaultFileName,
                content = jsonContent,
            ) ?: return

            result
                .onSuccess { savedFile ->
                    currentProjectFile = savedFile.file
                    _uiState.update {
                        it.copy(projectName = savedFile.projectName)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            fileDialogAlert = FileDialogAlert.SaveError(details = error.message),
                        )
                    }
                }
        }
    }

    private suspend inline fun withFileDialogLock(block: () -> Unit) {
        if (_uiState.value.isFileDialogOpen) return
        _uiState.update { it.copy(isFileDialogOpen = true) }
        try {
            block()
        } finally {
            _uiState.update { it.copy(isFileDialogOpen = false) }
        }
    }

    fun dismissFileDialogAlert() {
        _uiState.update {
            it.copy(fileDialogAlert = null)
        }
    }

    fun dismissCalculationError() {
        _uiState.update { current ->
            if (current.calculationState is CalculationState.Error) {
                current.copy(calculationState = CalculationState.Idle)
            } else {
                current
            }
        }
    }

    fun loadProjectFromJson(
        jsonString: String,
        projectName: String? = null,
        projectFile: ProjectFileHandle? = null,
    ): Result<NormalizationResult> = runCatching {
        val normalizationResult = ProjectFileCodec.decodeAndNormalize(jsonString)

        undoStack.clear()
        redoStack.clear()
        currentProjectFile = projectFile

        applyConstructionState(
            construction = normalizationResult.construction,
            projectName = projectName,
            selectedRodIndex = null,
            selectedNodeIndex = null,
            fileDialogAlert = if (normalizationResult.wasModified) {
                FileDialogAlert.NormalizedOnLoad(normalizationResult.changes)
            } else {
                null
            },
            incrementLoadVersion = true,
        )

        normalizationResult
    }.onFailure { error ->
        _uiState.update {
            it.copy(
                fileDialogAlert = FileDialogAlert.LoadError(details = error.message),
            )
        }
    }

    fun exportProjectToJson(): String =
        ProjectFileCodec.encode(_uiState.value.construction)

    fun undo() {
        if (undoStack.isEmpty()) return

        val current = _uiState.value
        val previousConstruction = undoStack.removeLast()
        redoStack.addLast(current.construction)

        applyConstructionState(
            construction = previousConstruction,
            selectedRodIndex = current.selectedRodIndex,
            selectedNodeIndex = current.selectedNodeIndex,
        )
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val current = _uiState.value
        val nextConstruction = redoStack.removeLast()
        undoStack.addLast(current.construction)

        applyConstructionState(
            construction = nextConstruction,
            selectedRodIndex = current.selectedRodIndex,
            selectedNodeIndex = current.selectedNodeIndex,
        )
    }

    private fun performCalculation(targetStage: AppStage) {
        val current = _uiState.value

        val issues = ConstructionValidator.validate(current.construction)

        if (issues.any { it.isError }) {
            _uiState.update {
                it.copy(
                    validationIssues = issues,
                    isCalculated = false,
                    calculationState = CalculationState.Error(
                        validationIssue = issues.first { issue -> issue.isError },
                    ),
                )
            }

            return
        }

        calculateDisplacements(current.construction)
            .onSuccess { displacementsArray ->
                val reactions = calculateReactions(current.construction, displacementsArray)
                val residual = calculateEquilibriumResidual(current.construction, reactions)
                val strengths = analyzeStrength(current.construction, displacementsArray)

                _uiState.update {
                    val defaultRodIndex = it.selectedRodIndex
                        ?.takeIf { idx -> idx in current.construction.rods.indices }
                        ?: if (current.construction.rods.isNotEmpty()) 0 else null
                    val defaultLocalX = if (defaultRodIndex != null) {
                        it.inspectedLocalX.coerceIn(0.0, current.construction.rods[defaultRodIndex].length)
                    } else {
                        0.0
                    }

                    it.copy(
                        selectedStage = targetStage,
                        isCalculated = true,
                        validationIssues = issues,
                        inspectedRodIndex = defaultRodIndex,
                        inspectedLocalX = defaultLocalX,
                        calculationState = CalculationState.Success(
                            displacements = displacementsArray.toList(),
                            reactions = reactions,
                            equilibriumResidual = residual,
                            rodStrengths = strengths,
                        ),
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCalculated = false,
                        validationIssues = issues,
                        calculationState = CalculationState.Error(
                            message = error.message,
                        ),
                    )
                }
            }
    }

    private fun modifyConstruction(
        newConstruction: Construction,
        projectName: String? = _uiState.value.projectName,
        transformSelectedRod: (Int?) -> Int? = { it },
        transformSelectedNode: (Int?) -> Int? = { it },
    ) {
        val current = _uiState.value
        if (newConstruction == current.construction) return

        if (undoStack.size >= MAX_HISTORY_SIZE) {
            undoStack.removeFirst()
        }
        undoStack.addLast(current.construction)
        redoStack.clear()

        applyConstructionState(
            construction = newConstruction,
            projectName = projectName,
            selectedRodIndex = transformSelectedRod(current.selectedRodIndex),
            selectedNodeIndex = transformSelectedNode(current.selectedNodeIndex),
        )
    }

    private fun applyConstructionState(
        construction: Construction,
        selectedRodIndex: Int?,
        selectedNodeIndex: Int?,
        projectName: String? = _uiState.value.projectName,
        fileDialogAlert: FileDialogAlert? = _uiState.value.fileDialogAlert,
        incrementLoadVersion: Boolean = false,
    ) {
        val validRodIndex = selectedRodIndex?.takeIf { it in construction.rods.indices }
        val validNodeIndex = if (construction.rods.isEmpty()) {
            null
        } else {
            selectedNodeIndex?.takeIf { it in 0 until construction.nodeCount }
        }
        val issues = ConstructionValidator.validate(construction)

        _uiState.update {
            it.copy(
                construction = construction,
                projectName = projectName,
                selectedStage = AppStage.Preprocessor,
                isCalculated = false,
                selectedRodIndex = validRodIndex,
                selectedNodeIndex = validNodeIndex,
                validationIssues = issues,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                calculationState = CalculationState.Idle,
                fileDialogAlert = fileDialogAlert,
                projectLoadVersion = if (incrementLoadVersion) it.projectLoadVersion + 1 else it.projectLoadVersion,
            )
        }
    }

    private fun createInitialState(): MainUiState {
        val initialConstruction = Construction.defaultDemoConstruction()

        return MainUiState(
            construction = initialConstruction,
            validationIssues = ConstructionValidator.validate(initialConstruction),
        )
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
}
