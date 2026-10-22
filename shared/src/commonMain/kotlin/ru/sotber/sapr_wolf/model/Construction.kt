package ru.sotber.sapr_wolf.model

import kotlinx.serialization.Serializable

@Serializable
data class Construction(
    val version: Int = 1,
    val rods: List<Rod> = emptyList(),
    val nodeForces: List<Double> = emptyList(),
    val fixedNodes: Set<Int> = emptySet(),
)

val Construction.nodeCount: Int get() = rods.size + 1

fun Construction.nodePositionOnAxis(nodeIndex: Int): Double = rods.take(nodeIndex).sumOf { it.length }
