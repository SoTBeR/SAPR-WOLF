package ru.sotber.sapr_wolf.model

import kotlinx.serialization.Serializable

@Serializable
data class Rod(
    val length: Double,
    val crossSectionArea: Double,
    val elasticModulus: Double,
    val allowableStress: Double,
    val distributedLoad: Double = 0.0,
)
