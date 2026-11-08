package ru.sotber.sapr_wolf.preprocessor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.nodeCount
import sapr_wolf.shared.generated.resources.*

sealed interface NormalizationChange {
    data object ClearedGarbageForEmptyRods : NormalizationChange
    data class PaddedNodeForces(val addedCount: Int) : NormalizationChange
    data class TruncatedNodeForces(val removedCount: Int) : NormalizationChange
    data class RemovedOutOfRangeSupports(val invalidIndices: List<Int>) : NormalizationChange
    data class ReplacedInvalidNodeForce(val nodeNumber: Int, val defaultValue: Double) : NormalizationChange
    data class ReplacedInvalidRodLength(val rodNumber: Int, val defaultValue: Double) : NormalizationChange
    data class ReplacedInvalidRodArea(val rodNumber: Int, val defaultValue: Double) : NormalizationChange
    data class ReplacedInvalidRodModulus(val rodNumber: Int, val defaultValue: Double) : NormalizationChange
    data class ReplacedInvalidRodAllowableStress(val rodNumber: Int, val defaultValue: Double) : NormalizationChange
    data class ReplacedInvalidRodDistributedLoad(val rodNumber: Int, val defaultValue: Double) : NormalizationChange
}

data class NormalizationResult(
    val construction: Construction,
    val changes: List<NormalizationChange>,
) {
    val wasModified: Boolean get() = changes.isNotEmpty()
}

@Composable
fun NormalizationChange.localizedMessage(): String = when (this) {
    NormalizationChange.ClearedGarbageForEmptyRods ->
        stringResource(Res.string.norm_cleared_empty_construction_garbage)
    is NormalizationChange.PaddedNodeForces ->
        stringResource(Res.string.norm_padded_node_forces, addedCount)
    is NormalizationChange.TruncatedNodeForces ->
        stringResource(Res.string.norm_truncated_node_forces, removedCount)
    is NormalizationChange.RemovedOutOfRangeSupports ->
        stringResource(Res.string.norm_removed_invalid_supports, invalidIndices.joinToString(", "))
    is NormalizationChange.ReplacedInvalidNodeForce ->
        stringResource(Res.string.norm_replaced_node_force, nodeNumber, defaultValue.toString())
    is NormalizationChange.ReplacedInvalidRodLength ->
        stringResource(Res.string.norm_replaced_rod_length, rodNumber, defaultValue.toString())
    is NormalizationChange.ReplacedInvalidRodArea ->
        stringResource(Res.string.norm_replaced_rod_area, rodNumber, defaultValue.toString())
    is NormalizationChange.ReplacedInvalidRodModulus ->
        stringResource(Res.string.norm_replaced_rod_modulus, rodNumber, defaultValue.toString())
    is NormalizationChange.ReplacedInvalidRodAllowableStress ->
        stringResource(Res.string.norm_replaced_rod_allowable_stress, rodNumber, defaultValue.toString())
    is NormalizationChange.ReplacedInvalidRodDistributedLoad ->
        stringResource(Res.string.norm_replaced_rod_distributed_load, rodNumber, defaultValue.toString())
}

object ConstructionNormalizer {
    const val DEFAULT_LENGTH = 1.0
    const val DEFAULT_AREA = 1.0
    const val DEFAULT_MODULUS = 1.0
    const val DEFAULT_ALLOWABLE_STRESS = 160.0
    const val DEFAULT_DISTRIBUTED_LOAD = 0.0
    const val DEFAULT_NODE_FORCE = 0.0

    fun normalize(construction: Construction): NormalizationResult {
        val changes = mutableListOf<NormalizationChange>()

        if (construction.rods.isEmpty()) {
            if (construction.nodeForces.isNotEmpty() || construction.fixedNodes.isNotEmpty()) {
                changes.add(NormalizationChange.ClearedGarbageForEmptyRods)
            }
            return NormalizationResult(
                construction = construction.copy(
                    nodeForces = emptyList(),
                    fixedNodes = emptySet(),
                ),
                changes = changes,
            )
        }

        val totalNodes = construction.nodeCount

        val normalizedRods = construction.rods.mapIndexed { rodIndex, rod ->
            val rodNumber = rodIndex + 1

            val validLength = if (rod.length.isFinite() && rod.length > 0.0) {
                rod.length
            } else {
                changes.add(NormalizationChange.ReplacedInvalidRodLength(rodNumber, DEFAULT_LENGTH))
                DEFAULT_LENGTH
            }

            val validArea = if (rod.crossSectionArea.isFinite() && rod.crossSectionArea > 0.0) {
                rod.crossSectionArea
            } else {
                changes.add(NormalizationChange.ReplacedInvalidRodArea(rodNumber, DEFAULT_AREA))
                DEFAULT_AREA
            }

            val validModulus = if (rod.elasticModulus.isFinite() && rod.elasticModulus > 0.0) {
                rod.elasticModulus
            } else {
                changes.add(NormalizationChange.ReplacedInvalidRodModulus(rodNumber, DEFAULT_MODULUS))
                DEFAULT_MODULUS
            }

            val validAllowableStress = if (rod.allowableStress.isFinite() && rod.allowableStress > 0.0) {
                rod.allowableStress
            } else {
                changes.add(NormalizationChange.ReplacedInvalidRodAllowableStress(rodNumber, DEFAULT_ALLOWABLE_STRESS))
                DEFAULT_ALLOWABLE_STRESS
            }

            val validDistributedLoad = if (rod.distributedLoad.isFinite()) {
                rod.distributedLoad
            } else {
                changes.add(NormalizationChange.ReplacedInvalidRodDistributedLoad(rodNumber, DEFAULT_DISTRIBUTED_LOAD))
                DEFAULT_DISTRIBUTED_LOAD
            }

            Rod(
                length = validLength,
                crossSectionArea = validArea,
                elasticModulus = validModulus,
                allowableStress = validAllowableStress,
                distributedLoad = validDistributedLoad,
            )
        }

        if (construction.nodeForces.size < totalNodes) {
            changes.add(NormalizationChange.PaddedNodeForces(totalNodes - construction.nodeForces.size))
        } else if (construction.nodeForces.size > totalNodes) {
            changes.add(NormalizationChange.TruncatedNodeForces(construction.nodeForces.size - totalNodes))
        }

        val normalizedForces = List(totalNodes) { nodeIndex ->
            val rawForce = construction.nodeForces.getOrElse(nodeIndex) { DEFAULT_NODE_FORCE }
            if (rawForce.isFinite()) {
                rawForce
            } else {
                changes.add(NormalizationChange.ReplacedInvalidNodeForce(nodeIndex + 1, DEFAULT_NODE_FORCE))
                DEFAULT_NODE_FORCE
            }
        }

        val (validSupports, invalidSupports) = construction.fixedNodes.partition { it in 0 until totalNodes }
        if (invalidSupports.isNotEmpty()) {
            changes.add(NormalizationChange.RemovedOutOfRangeSupports(invalidSupports.sorted()))
        }

        return NormalizationResult(
            construction = construction.copy(
                rods = normalizedRods,
                nodeForces = normalizedForces,
                fixedNodes = validSupports.toSet(),
            ),
            changes = changes,
        )
    }
}
