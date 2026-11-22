package ru.sotber.sapr_wolf.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectFile(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val construction: Construction,
    val calculation: SavedCalculation? = null,
    val hash: CalculationHash? = null,
) {
    companion object {
        const val LEGACY_FORMAT_VERSION = 1
        const val CURRENT_FORMAT_VERSION = 2
    }
}

@Serializable
data class SavedCalculation(
    val solverVersion: Int = CURRENT_SOLVER_VERSION,
    val displacements: List<Double>,
    val reactions: Map<Int, Double>,
    val equilibriumResidual: Double,
    val rodStrengths: List<RodStrength>,
) {
    companion object {
        const val CURRENT_SOLVER_VERSION = 1
    }
}

@Serializable
data class CalculationHash(
    val algorithm: String = ALGORITHM_HMAC_SHA256,
    val digest: String,
) {
    companion object {
        const val ALGORITHM_HMAC_SHA256 = "HMAC-SHA256"
    }
}
