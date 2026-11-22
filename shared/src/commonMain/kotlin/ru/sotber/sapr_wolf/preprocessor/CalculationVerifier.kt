package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.CalculationHash
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.SavedCalculation
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.postprocessor.analyzeStrength
import ru.sotber.sapr_wolf.postprocessor.calculateEquilibriumResidual
import ru.sotber.sapr_wolf.postprocessor.calculateReactions
import ru.sotber.sapr_wolf.processor.applyFixedNodes
import ru.sotber.sapr_wolf.processor.assembleGlobalSystem
import kotlin.math.abs
import kotlin.math.max

enum class CalculationInvalidationReason {
    NormalizedConstruction,
    OutdatedSolverVersion,
    HashMismatch,
    MathVerificationFailed,
}

sealed interface CalculationVerificationResult {
    data object Valid : CalculationVerificationResult
    data class Invalid(val reason: CalculationInvalidationReason) : CalculationVerificationResult
}

object CalculationVerifier {
    private const val SUPPORT_TOLERANCE = 1e-9
    private const val LINEAR_SYSTEM_TOLERANCE = 1e-7
    private const val DERIVED_VALUE_TOLERANCE = 1e-7

    fun verify(
        construction: Construction,
        calculation: SavedCalculation,
        hash: CalculationHash?,
        wasNormalized: Boolean,
    ): CalculationVerificationResult {
        if (wasNormalized) {
            return CalculationVerificationResult.Invalid(CalculationInvalidationReason.NormalizedConstruction)
        }
        if (calculation.solverVersion != SavedCalculation.CURRENT_SOLVER_VERSION) {
            return CalculationVerificationResult.Invalid(CalculationInvalidationReason.OutdatedSolverVersion)
        }
        if (!ProjectHash.verifyHash(construction, calculation, hash)) {
            return CalculationVerificationResult.Invalid(CalculationInvalidationReason.HashMismatch)
        }
        if (!verifyMath(construction, calculation)) {
            return CalculationVerificationResult.Invalid(CalculationInvalidationReason.MathVerificationFailed)
        }
        return CalculationVerificationResult.Valid
    }

    internal fun verifyMath(
        construction: Construction,
        calculation: SavedCalculation,
    ): Boolean {
        if (construction.rods.isEmpty() || construction.fixedNodes.isEmpty()) return false
        if (ConstructionValidator.validate(construction).any { it.isError }) return false

        val totalNodes = construction.nodeCount
        if (calculation.displacements.size != totalNodes) return false
        if (calculation.displacements.any { !it.isFinite() }) return false
        if (calculation.rodStrengths.size != construction.rods.size) return false
        if (!calculation.equilibriumResidual.isFinite()) return false

        for (fixedNode in construction.fixedNodes) {
            if (fixedNode !in 0 until totalNodes) return false
            if (abs(calculation.displacements[fixedNode]) > SUPPORT_TOLERANCE) return false
        }

        val initialSystem = assembleGlobalSystem(construction)
        val system = applyFixedNodes(initialSystem, construction.fixedNodes)

        for (row in 0 until system.size) {
            var lhs = 0.0
            var rowMagnitude = abs(system.rightPart[row])
            for (col in 0 until system.size) {
                val term = system.matrix[row][col] * calculation.displacements[col]
                lhs += term
                rowMagnitude += abs(term)
            }
            val residual = abs(lhs - system.rightPart[row])
            val tolerance = LINEAR_SYSTEM_TOLERANCE * max(1.0, rowMagnitude)
            if (!residual.isFinite() || residual > tolerance) return false
        }

        val displacementsArray = calculation.displacements.toDoubleArray()
        val expectedReactions = calculateReactions(construction, displacementsArray)
        if (calculation.reactions.keys != expectedReactions.keys) return false
        for ((nodeIndex, expectedValue) in expectedReactions) {
            val savedValue = calculation.reactions[nodeIndex] ?: return false
            if (!approxEqual(savedValue, expectedValue, DERIVED_VALUE_TOLERANCE)) return false
        }

        val expectedResidual = calculateEquilibriumResidual(construction, expectedReactions)
        if (!approxEqual(calculation.equilibriumResidual, expectedResidual, DERIVED_VALUE_TOLERANCE)) {
            return false
        }

        val expectedStrengths = analyzeStrength(construction, displacementsArray)
        if (calculation.rodStrengths.size != expectedStrengths.size) return false
        for (i in expectedStrengths.indices) {
            val saved = calculation.rodStrengths[i]
            val expected = expectedStrengths[i]
            if (saved.rodIndex != expected.rodIndex) return false
            if (saved.isSatisfied != expected.isSatisfied) return false
            if (!approxEqual(saved.maxStress, expected.maxStress, DERIVED_VALUE_TOLERANCE)) return false
            if (!approxEqual(saved.allowableStress, expected.allowableStress, DERIVED_VALUE_TOLERANCE)) return false
            if (!approxEqualOrBothInfinite(saved.safetyFactor, expected.safetyFactor, DERIVED_VALUE_TOLERANCE)) {
                return false
            }
        }

        return true
    }

    private fun approxEqual(a: Double, b: Double, tolerance: Double): Boolean {
        if (!a.isFinite() || !b.isFinite()) return false
        val scale = max(1.0, max(abs(a), abs(b)))
        return abs(a - b) <= tolerance * scale
    }

    private fun approxEqualOrBothInfinite(a: Double, b: Double, tolerance: Double): Boolean {
        if (a == Double.POSITIVE_INFINITY && b == Double.POSITIVE_INFINITY) return true
        if (a == Double.NEGATIVE_INFINITY && b == Double.NEGATIVE_INFINITY) return true
        return approxEqual(a, b, tolerance)
    }
}
