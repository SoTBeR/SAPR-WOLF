package ru.sotber.sapr_wolf.processor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.LinearSystem
import ru.sotber.sapr_wolf.model.nodeCount

fun assembleGlobalSystem(construction: Construction): LinearSystem {
    val totalNodes = construction.nodeCount
    val matrix = Array(totalNodes) { DoubleArray(totalNodes) }
    val rightPart = DoubleArray(totalNodes)

    for (nodeIndex in 0 until totalNodes) {
        if (nodeIndex < construction.nodeForces.size) {
            rightPart[nodeIndex] = construction.nodeForces[nodeIndex]
        }
    }

    for (rodIndex in construction.rods.indices) {
        val rod = construction.rods[rodIndex]
        val stiffness = (rod.elasticModulus * rod.crossSectionArea) / rod.length
        val rightNodeIndex = rodIndex + 1

        matrix[rodIndex][rodIndex] += stiffness
        matrix[rodIndex][rightNodeIndex] -= stiffness
        matrix[rightNodeIndex][rodIndex] -= stiffness
        matrix[rightNodeIndex][rightNodeIndex] += stiffness

        val equivalentNodalLoad = (rod.distributedLoad * rod.length) / 2.0
        rightPart[rodIndex] += equivalentNodalLoad
        rightPart[rightNodeIndex] += equivalentNodalLoad
    }

    return LinearSystem(matrix = matrix, rightPart = rightPart)
}
fun applyFixedNodes(system: LinearSystem, fixedNodes: Set<Int>): LinearSystem {
    val modifiedSystem = system.copy()
    val totalNodes = modifiedSystem.size

    for (fixedNodeIndex in fixedNodes) {
        if (fixedNodeIndex !in 0 until totalNodes) continue

        for (otherNodeIndex in 0 until totalNodes) {
            modifiedSystem.matrix[fixedNodeIndex][otherNodeIndex] = 0.0
            modifiedSystem.matrix[otherNodeIndex][fixedNodeIndex] = 0.0
        }

        modifiedSystem.matrix[fixedNodeIndex][fixedNodeIndex] = 1.0
        modifiedSystem.rightPart[fixedNodeIndex] = 0.0
    }

    return modifiedSystem
}
