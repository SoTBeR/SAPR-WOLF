package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.processor.assembleGlobalSystem

fun calculateReactions(construction: Construction, displacements: DoubleArray): Map<Int, Double> {
    val initialSystem = assembleGlobalSystem(construction)
    val totalNodes = initialSystem.size
    val reactions = mutableMapOf<Int, Double>()

    for (fixedNodeIndex in construction.fixedNodes) {
        if (fixedNodeIndex in 0 until totalNodes) {
            var reaction = -initialSystem.rightPart[fixedNodeIndex]
            for (nodeIndex in 0 until totalNodes) {
                reaction += initialSystem.matrix[fixedNodeIndex][nodeIndex] * displacements[nodeIndex]
            }
            reactions[fixedNodeIndex] = reaction
        }
    }

    return reactions
}

fun calculateEquilibriumResidual(construction: Construction, reactions: Map<Int, Double>): Double {
    val sumReactions = reactions.values.sum()
    val sumNodeForces = construction.nodeForces.sum()
    val sumDistributedLoads = construction.rods.sumOf { it.distributedLoad * it.length }
    return sumReactions + sumNodeForces + sumDistributedLoads
}
