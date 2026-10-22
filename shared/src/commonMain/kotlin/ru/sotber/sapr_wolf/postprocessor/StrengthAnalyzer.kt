package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.RodStrength
import kotlin.math.abs
import kotlin.math.max

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
