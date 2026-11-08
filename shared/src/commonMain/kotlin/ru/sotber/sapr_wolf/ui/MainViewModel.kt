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
import ru.sotber.sapr_wolf.postprocessor.analyzeStrength
import ru.sotber.sapr_wolf.postprocessor.calculateEquilibriumResidual
import ru.sotber.sapr_wolf.postprocessor.calculateReactions
import ru.sotber.sapr_wolf.preprocessor.*
import ru.sotber.sapr_wolf.processor.calculateDisplacements
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.dialog_default_project_filename

class MainViewModel : ViewModel() {
    private val undoStack = ArrayDeque<Construction>()
    private val redoStack = ArrayDeque<Construction>()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectRod(rodIndex: Int) {
        val current = _uiState.value

        if (rodIndex !in current.construction.rods.indices) return

        _uiState.update {
            it.copy(
                selectedRodIndex = rodIndex,
                selectedNodeIndex = null,
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

    fun calculate() {
        if (!_uiState.value.canCalculate) return

        performCalculation(targetStage = AppStage.Postprocessor)
    }

    fun addRodLeft() {
        val construction = _uiState.value.construction
        val firstRod = construction.rods.firstOrNull()
        val newRod = firstRod?.copy(distributedLoad = 0.0) ?: defaultRod()

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
        val newRod = lastRod?.copy(distributedLoad = 0.0) ?: defaultRod()

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

    fun startNewProject() {
        modifyConstruction(
            newConstruction = Construction(),
            transformSelectedRod = { null },
            transformSelectedNode = { null },
        )
    }

    fun openProjectFromFile() {
        viewModelScope.launch {
            val result = ProjectFileDialog.pickJsonFileToOpen() ?: return@launch

            result
                .onSuccess { jsonContent ->
                    loadProjectFromJson(jsonContent)
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

    fun saveProjectToFile() {
        viewModelScope.launch {
            val defaultFileName = runCatching { getString(Res.string.dialog_default_project_filename) }
                .getOrDefault("project.json")
            val jsonContent = exportProjectToJson()

            val result = ProjectFileDialog.saveJsonFile(
                defaultFileName = defaultFileName,
                content = jsonContent,
            ) ?: return@launch

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        fileDialogAlert = FileDialogAlert.SaveError(details = error.message),
                    )
                }
            }
        }
    }

    fun dismissFileDialogAlert() {
        _uiState.update {
            it.copy(fileDialogAlert = null)
        }
    }

    fun loadProjectFromJson(jsonString: String): Result<NormalizationResult> = runCatching {
        val normalizationResult = ProjectFileCodec.decodeAndNormalize(jsonString)
        undoStack.clear()
        redoStack.clear()
        applyConstructionState(
            construction = normalizationResult.construction,
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
                    it.copy(
                        selectedStage = targetStage,
                        isCalculated = true,
                        validationIssues = issues,
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
            selectedRodIndex = transformSelectedRod(current.selectedRodIndex),
            selectedNodeIndex = transformSelectedNode(current.selectedNodeIndex),
        )
    }

    private fun applyConstructionState(
        construction: Construction,
        selectedRodIndex: Int?,
        selectedNodeIndex: Int?,
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
        val initialConstruction = defaultDemoConstruction()

        return MainUiState(
            construction = initialConstruction,
            validationIssues = ConstructionValidator.validate(initialConstruction),
        )
    }

    private fun defaultRod(): Rod = Rod(
        length = ConstructionNormalizer.DEFAULT_LENGTH,
        crossSectionArea = ConstructionNormalizer.DEFAULT_AREA,
        elasticModulus = ConstructionNormalizer.DEFAULT_MODULUS,
        allowableStress = ConstructionNormalizer.DEFAULT_ALLOWABLE_STRESS,
    )

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
}
