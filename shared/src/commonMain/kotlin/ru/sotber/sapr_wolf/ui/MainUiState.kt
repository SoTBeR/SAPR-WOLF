package ru.sotber.sapr_wolf.ui

import ru.sotber.sapr_wolf.model.AppStage
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.RodStrength
import ru.sotber.sapr_wolf.model.ValidationIssue

sealed interface CalculationState {
    data object Idle : CalculationState

    data class Success(
        val displacements: List<Double>,
        val reactions: Map<Int, Double>,
        val equilibriumResidual: Double,
        val rodStrengths: List<RodStrength>,
    ) : CalculationState

    data class Error(
        val message: String,
    ) : CalculationState
}

data class MainUiState(
    val construction: Construction = defaultDemoConstruction(),
    val selectedStage: AppStage = AppStage.Preprocessor,
    val isCalculated: Boolean = false,
    val selectedRodIndex: Int? = null,
    val selectedNodeIndex: Int? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
) {
    val canCalculate: Boolean
        get() = validationIssues.none { it.isError }

    val isProcessorEnabled: Boolean
        get() = isCalculated || canCalculate

    val isPostprocessorEnabled: Boolean
        get() = isCalculated
}

fun defaultDemoConstruction(): Construction = Construction(
    rods = listOf(
        Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 1.0),
        Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
        Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
        Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = -1.0),
    ),
    nodeForces = listOf(0.0, 0.0, 1.0, 0.0, 0.0),
    fixedNodes = setOf(0, 4),
)
