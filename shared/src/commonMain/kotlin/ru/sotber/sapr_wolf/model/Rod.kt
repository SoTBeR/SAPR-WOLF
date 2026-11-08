package ru.sotber.sapr_wolf.model

import kotlinx.serialization.Serializable

@Serializable
data class Rod(
    val length: Double,
    val crossSectionArea: Double,
    val elasticModulus: Double,
    val allowableStress: Double,
    val distributedLoad: Double = DEFAULT_DISTRIBUTED_LOAD,
) {
    companion object {
        const val DEFAULT_LENGTH = 1.0
        const val DEFAULT_AREA = 1.0
        const val DEFAULT_MODULUS = 1.0
        const val DEFAULT_ALLOWABLE_STRESS = 160.0
        const val DEFAULT_DISTRIBUTED_LOAD = 0.0

        fun defaultRod(): Rod = Rod(
            length = DEFAULT_LENGTH,
            crossSectionArea = DEFAULT_AREA,
            elasticModulus = DEFAULT_MODULUS,
            allowableStress = DEFAULT_ALLOWABLE_STRESS,
            distributedLoad = DEFAULT_DISTRIBUTED_LOAD,
        )
    }
}
