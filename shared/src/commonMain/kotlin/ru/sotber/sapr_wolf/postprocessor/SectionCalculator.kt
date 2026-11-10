package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.SectionResult

fun calculateSection(
    construction: Construction,
    displacements: DoubleArray,
    rodIndex: Int,
    localX: Double
): SectionResult {
    require(rodIndex in construction.rods.indices) {
        "Индекс стержня $rodIndex выходит за пределы [0, ${construction.rods.size - 1}]"
    }

    val rod = construction.rods[rodIndex]
    val clampedLocalX = localX.coerceIn(0.0, rod.length)

    val leftNodeDisplacement = displacements[rodIndex]
    val rightNodeDisplacement = displacements[rodIndex + 1]
    val deltaDisplacement = rightNodeDisplacement - leftNodeDisplacement

    val longitudinalStiffness = (rod.elasticModulus * rod.crossSectionArea) / rod.length
    val axialForce = longitudinalStiffness * deltaDisplacement + rod.distributedLoad * (rod.length / 2.0 - clampedLocalX)

    val normalStress = axialForce / rod.crossSectionArea

    val displacement = leftNodeDisplacement + deltaDisplacement * (clampedLocalX / rod.length) +
            (rod.distributedLoad * clampedLocalX * (rod.length - clampedLocalX)) / (2.0 * rod.elasticModulus * rod.crossSectionArea)

    return SectionResult(
        axialForce = axialForce,
        normalStress = normalStress,
        displacement = displacement,
    )
}
