package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.ValidationIssue
import ru.sotber.sapr_wolf.model.ValidationIssueType
import ru.sotber.sapr_wolf.model.nodeCount
import kotlin.math.abs

object ConstructionValidator {

    fun validate(construction: Construction): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val totalNodes = construction.nodeCount

        if (construction.rods.isEmpty()) {
            issues.add(
                ValidationIssue(
                    type = ValidationIssueType.NO_RODS,
                )
            )

            return issues
        }

        if (construction.nodeForces.isNotEmpty() && construction.nodeForces.size != totalNodes) {
            issues.add(
                ValidationIssue(
                    type = ValidationIssueType.FORCES_SIZE_MISMATCH,
                    actualForcesCount = construction.nodeForces.size,
                    expectedNodesCount = totalNodes,
                )
            )
        }

        for (rodIndex in construction.rods.indices) {
            val rod = construction.rods[rodIndex]

            val hasInvalidNumberInRod = !rod.length.isFinite() ||
                    !rod.crossSectionArea.isFinite() ||
                    !rod.elasticModulus.isFinite() ||
                    !rod.allowableStress.isFinite() ||
                    !rod.distributedLoad.isFinite()

            if (hasInvalidNumberInRod) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.NON_FINITE_NUMBER,
                        rodIndex = rodIndex,
                    )
                )
            }

            if (rod.length <= 0.0) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.INVALID_ROD_LENGTH,
                        rodIndex = rodIndex,
                    )
                )
            }

            if (rod.crossSectionArea <= 0.0) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.INVALID_ROD_AREA,
                        rodIndex = rodIndex,
                    )
                )
            }

            if (rod.elasticModulus <= 0.0) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.INVALID_ROD_MODULUS,
                        rodIndex = rodIndex,
                    )
                )
            }

            if (rod.allowableStress <= 0.0) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.INVALID_ROD_ALLOWABLE_STRESS,
                        rodIndex = rodIndex,
                    )
                )
            }
        }

        for (nodeIndex in construction.nodeForces.indices) {
            val force = construction.nodeForces[nodeIndex]
            if (!force.isFinite()) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.NON_FINITE_NUMBER,
                        nodeIndex = nodeIndex,
                    )
                )
            }
        }

        if (construction.fixedNodes.isEmpty()) {
            issues.add(
                ValidationIssue(
                    type = ValidationIssueType.NO_SUPPORTS,
                )
            )
        }

        for (fixedNodeIndex in construction.fixedNodes) {
            if (fixedNodeIndex !in 0 until totalNodes) {
                issues.add(
                    ValidationIssue(
                        type = ValidationIssueType.SUPPORT_INDEX_OUT_OF_RANGE,
                        nodeIndex = fixedNodeIndex,
                        expectedNodesCount = totalNodes,
                    )
                )
            }
        }

        val hasAnyNodeForce = construction.nodeForces.any { it.isFinite() && abs(it) > 1e-12 }
        val hasAnyDistributedLoad = construction.rods.any { it.distributedLoad.isFinite() && abs(it.distributedLoad) > 1e-12 }
        if (!hasAnyNodeForce && !hasAnyDistributedLoad) {
            issues.add(
                ValidationIssue(
                    type = ValidationIssueType.NO_LOADS,
                )
            )
        }

        for (fixedNodeIndex in construction.fixedNodes) {
            if (fixedNodeIndex in construction.nodeForces.indices) {
                val forceAtSupport = construction.nodeForces[fixedNodeIndex]
                if (forceAtSupport.isFinite() && abs(forceAtSupport) > 1e-12) {
                    issues.add(
                        ValidationIssue(
                            type = ValidationIssueType.FORCE_AT_SUPPORT,
                            nodeIndex = fixedNodeIndex,
                        )
                    )
                }
            }
        }

        return issues
    }

    fun hasErrors(construction: Construction): Boolean =
        validate(construction).any { it.isError }
}
