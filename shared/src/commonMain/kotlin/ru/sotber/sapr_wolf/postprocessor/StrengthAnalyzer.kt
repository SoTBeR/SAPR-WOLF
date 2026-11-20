package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.RodStrength
import kotlin.math.abs
import kotlin.math.max

data class PostprocessorSummary(
    val mostLoadedRodIndex: Int?,
    val minSafetyFactor: Double,
    val overloadedRodIndices: List<Int>,
    val maxAbsAxialForce: Double,
    val maxAbsNormalStress: Double,
    val maxAbsDisplacement: Double,
)

fun analyzeStrength(construction: Construction, displacements: DoubleArray): List<RodStrength> {
    return construction.rods.indices.map { rodIndex ->
        val rod = construction.rods[rodIndex]

        val leftSection = calculateSection(construction, displacements, rodIndex, 0.0)
        val rightSection = calculateSection(construction, displacements, rodIndex, rod.length)

        val maxStress = max(abs(leftSection.normalStress), abs(rightSection.normalStress))
        val safetyFactor = if (maxStress > 1e-12) rod.allowableStress / maxStress else Double.POSITIVE_INFINITY
        val isSatisfied = maxStress <= rod.allowableStress + 1e-9

        RodStrength(
            rodIndex = rodIndex,
            maxStress = maxStress,
            allowableStress = rod.allowableStress,
            safetyFactor = safetyFactor,
            isSatisfied = isSatisfied,
        )
    }
}

fun summarizePostprocessor(
    construction: Construction,
    displacements: DoubleArray,
    rodStrengths: List<RodStrength> = analyzeStrength(construction, displacements),
): PostprocessorSummary {
    if (construction.rods.isEmpty()) {
        return PostprocessorSummary(
            mostLoadedRodIndex = null,
            minSafetyFactor = Double.POSITIVE_INFINITY,
            overloadedRodIndices = emptyList(),
            maxAbsAxialForce = 0.0,
            maxAbsNormalStress = 0.0,
            maxAbsDisplacement = 0.0,
        )
    }

    var maxAbsAxialForce = 0.0
    var maxAbsNormalStress = 0.0
    var maxAbsDisplacement = 0.0

    for (rodIndex in construction.rods.indices) {
        val rod = construction.rods[rodIndex]
        val leftSection = calculateSection(construction, displacements, rodIndex, 0.0)
        val rightSection = calculateSection(construction, displacements, rodIndex, rod.length)

        maxAbsAxialForce = max(maxAbsAxialForce, max(abs(leftSection.axialForce), abs(rightSection.axialForce)))
        maxAbsNormalStress = max(maxAbsNormalStress, max(abs(leftSection.normalStress), abs(rightSection.normalStress)))
        maxAbsDisplacement = max(maxAbsDisplacement, max(abs(leftSection.displacement), abs(rightSection.displacement)))

        if (abs(rod.distributedLoad) > 1e-12) {
            val deltaDisplacement = displacements[rodIndex + 1] - displacements[rodIndex]
            val extremumLocalX = rod.length / 2.0 +
                (rod.elasticModulus * rod.crossSectionArea * deltaDisplacement) / (rod.distributedLoad * rod.length)
            if (extremumLocalX > 0.0 && extremumLocalX < rod.length) {
                val extremumSection = calculateSection(construction, displacements, rodIndex, extremumLocalX)
                maxAbsDisplacement = max(maxAbsDisplacement, abs(extremumSection.displacement))
            }
        }
    }

    val overloadedRodIndices = rodStrengths
        .filter { !it.isSatisfied }
        .map { it.rodIndex }

    val mostLoaded = rodStrengths.minWithOrNull(
        compareBy<RodStrength> { it.safetyFactor }
            .thenByDescending { it.maxStress }
            .thenBy { it.rodIndex }
    )

    return PostprocessorSummary(
        mostLoadedRodIndex = mostLoaded?.rodIndex,
        minSafetyFactor = mostLoaded?.safetyFactor ?: Double.POSITIVE_INFINITY,
        overloadedRodIndices = overloadedRodIndices,
        maxAbsAxialForce = maxAbsAxialForce,
        maxAbsNormalStress = maxAbsNormalStress,
        maxAbsDisplacement = maxAbsDisplacement,
    )
}
