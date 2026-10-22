package ru.sotber.sapr_wolf.model

data class SectionResult(
    val axialForce: Double,
    val normalStress: Double,
    val displacement: Double,
)

data class RodStrength(
    val rodIndex: Int,
    val maxStress: Double,
    val allowableStress: Double,
    val safetyFactor: Double,
    val isSatisfied: Boolean,
)
