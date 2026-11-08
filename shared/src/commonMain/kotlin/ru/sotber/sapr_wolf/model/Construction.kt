package ru.sotber.sapr_wolf.model

import kotlinx.serialization.Serializable

@Serializable
data class Construction(
    val version: Int = 1,
    val rods: List<Rod> = emptyList(),
    val nodeForces: List<Double> = emptyList(),
    val fixedNodes: Set<Int> = emptySet(),
) {
    companion object {
        const val DEFAULT_NODE_FORCE = 0.0

        fun defaultDemoConstruction(): Construction = Construction(
            rods = listOf(
                Rod(length = 1.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 3.0),
                Rod(length = 3.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 160.0, distributedLoad = -5.0),
            ),
            nodeForces = listOf(0.0, 0.0, -1.0, 2.0, 0.0, 0.0),
            fixedNodes = setOf(0, 5),
        )
    }
}

val Construction.nodeCount: Int get() = rods.size + 1

fun Construction.nodePositionOnAxis(nodeIndex: Int): Double = rods.take(nodeIndex).sumOf { it.length }
