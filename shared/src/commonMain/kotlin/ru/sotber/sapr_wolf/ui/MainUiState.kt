package ru.sotber.sapr_wolf.ui

import ru.sotber.sapr_wolf.model.*
import ru.sotber.sapr_wolf.preprocessor.NormalizationChange

sealed interface CalculationState {
    data object Idle : CalculationState

    data class Success(
        val displacements: List<Double>,
        val reactions: Map<Int, Double>,
        val equilibriumResidual: Double,
        val rodStrengths: List<RodStrength>,
    ) : CalculationState

    data class Error(
        val validationIssue: ValidationIssue? = null,
        val message: String? = null,
    ) : CalculationState
}

sealed interface FileDialogAlert {
    data class NormalizedOnLoad(
        val changes: List<NormalizationChange>,
    ) : FileDialogAlert

    data class LoadError(
        val details: String? = null,
    ) : FileDialogAlert

    data class SaveError(
        val details: String? = null,
    ) : FileDialogAlert
}

data class MainUiState(
    val construction: Construction = Construction.defaultDemoConstruction(),
    val selectedStage: AppStage = AppStage.Preprocessor,
    val isCalculated: Boolean = false,
    val selectedRodIndex: Int? = null,
    val selectedNodeIndex: Int? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
    val fileDialogAlert: FileDialogAlert? = null,
    val projectLoadVersion: Int = 0,
) {
    val canCalculate: Boolean
        get() = validationIssues.none { it.isError }

    val isProcessorEnabled: Boolean
        get() = isCalculated || canCalculate

    val isPostprocessorEnabled: Boolean
        get() = isCalculated
}
