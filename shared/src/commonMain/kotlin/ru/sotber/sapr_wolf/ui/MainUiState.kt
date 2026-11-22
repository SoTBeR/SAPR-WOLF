package ru.sotber.sapr_wolf.ui

import ru.sotber.sapr_wolf.model.*
import ru.sotber.sapr_wolf.preprocessor.CalculationInvalidationReason
import ru.sotber.sapr_wolf.preprocessor.NormalizationChange
import ru.sotber.sapr_wolf.ui.components.EpureDisplayMode

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

    data class CalculationInvalidated(
        val reason: CalculationInvalidationReason,
        val normalizationChanges: List<NormalizationChange> = emptyList(),
    ) : FileDialogAlert

    data class ConfirmLegacyJsonConversion(
        val projectName: String,
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
    val projectName: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val selectedStage: AppStage = AppStage.Preprocessor,
    val isCalculated: Boolean = false,
    val selectedRodIndex: Int? = null,
    val selectedNodeIndex: Int? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
    val isFileDialogOpen: Boolean = false,
    val isExportReportDialogOpen: Boolean = false,
    val isHelpDialogOpen: Boolean = false,
    val fileDialogAlert: FileDialogAlert? = null,
    val projectLoadVersion: Int = 0,
    val epureDisplayMode: EpureDisplayMode = EpureDisplayMode.All,
    val inspectedRodIndex: Int? = null,
    val inspectedLocalX: Double = 0.0,
) {
    val canCalculate: Boolean
        get() = validationIssues.none { it.isError }

    val isProcessorEnabled: Boolean
        get() = isCalculated || canCalculate

    val isPostprocessorEnabled: Boolean
        get() = isCalculated
}
