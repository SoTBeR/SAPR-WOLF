package ru.sotber.sapr_wolf.model

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import sapr_wolf.shared.generated.resources.*

enum class ValidationSeverity {
    ERROR,
    WARNING,
}

enum class ValidationIssueType(
    val code: String,
    val severity: ValidationSeverity,
) {
    NO_RODS("E01", ValidationSeverity.ERROR),
    INVALID_ROD_LENGTH("E02", ValidationSeverity.ERROR),
    INVALID_ROD_AREA("E03", ValidationSeverity.ERROR),
    INVALID_ROD_MODULUS("E04", ValidationSeverity.ERROR),
    INVALID_ROD_ALLOWABLE_STRESS("E05", ValidationSeverity.ERROR),
    NO_SUPPORTS("E06", ValidationSeverity.ERROR),
    SUPPORT_INDEX_OUT_OF_RANGE("E07", ValidationSeverity.ERROR),
    NON_FINITE_NUMBER("E08", ValidationSeverity.ERROR),
    FORCES_SIZE_MISMATCH("E09", ValidationSeverity.ERROR),
    NO_LOADS("W01", ValidationSeverity.WARNING),
    FORCE_AT_SUPPORT("W02", ValidationSeverity.WARNING),
}

data class ValidationIssue(
    val type: ValidationIssueType,
    val rodIndex: Int? = null,
    val nodeIndex: Int? = null,
    val actualForcesCount: Int? = null,
    val expectedNodesCount: Int? = null,
) {
    val code: String get() = type.code
    val severity: ValidationSeverity get() = type.severity
    val isError: Boolean get() = severity == ValidationSeverity.ERROR
    val isWarning: Boolean get() = severity == ValidationSeverity.WARNING
}

@Composable
fun ValidationIssue.localizedMessage(): String {
    val rodNumber = (rodIndex ?: 0) + 1
    val nodeNumber = (nodeIndex ?: 0) + 1
    val maxNodeIndex = ((expectedNodesCount ?: 1) - 1).coerceAtLeast(0)

    return when (type) {
        ValidationIssueType.NO_RODS ->
            stringResource(Res.string.validation_e01_no_rods)
        ValidationIssueType.INVALID_ROD_LENGTH ->
            stringResource(Res.string.validation_e02_invalid_length, rodNumber)
        ValidationIssueType.INVALID_ROD_AREA ->
            stringResource(Res.string.validation_e03_invalid_area, rodNumber)
        ValidationIssueType.INVALID_ROD_MODULUS ->
            stringResource(Res.string.validation_e04_invalid_modulus, rodNumber)
        ValidationIssueType.INVALID_ROD_ALLOWABLE_STRESS ->
            stringResource(Res.string.validation_e05_invalid_allowable_stress, rodNumber)
        ValidationIssueType.NO_SUPPORTS ->
            stringResource(Res.string.validation_e06_no_supports)
        ValidationIssueType.SUPPORT_INDEX_OUT_OF_RANGE ->
            stringResource(Res.string.validation_e07_support_out_of_range, nodeIndex ?: 0, maxNodeIndex)
        ValidationIssueType.NON_FINITE_NUMBER ->
            if (rodIndex != null) {
                stringResource(Res.string.validation_e08_nan_in_rod, rodNumber)
            } else {
                stringResource(Res.string.validation_e08_nan_in_node, nodeNumber)
            }
        ValidationIssueType.FORCES_SIZE_MISMATCH ->
            stringResource(
                Res.string.validation_e09_forces_size_mismatch,
                actualForcesCount ?: 0,
                expectedNodesCount ?: 0,
            )
        ValidationIssueType.NO_LOADS ->
            stringResource(Res.string.validation_w01_no_loads)
        ValidationIssueType.FORCE_AT_SUPPORT ->
            stringResource(Res.string.validation_w02_force_at_support, nodeNumber)
    }
}

suspend fun ValidationIssue.resolveMessage(): String {
    val rodNumber = (rodIndex ?: 0) + 1
    val nodeNumber = (nodeIndex ?: 0) + 1
    val maxNodeIndex = ((expectedNodesCount ?: 1) - 1).coerceAtLeast(0)

    return when (type) {
        ValidationIssueType.NO_RODS ->
            getString(Res.string.validation_e01_no_rods)
        ValidationIssueType.INVALID_ROD_LENGTH ->
            getString(Res.string.validation_e02_invalid_length, rodNumber)
        ValidationIssueType.INVALID_ROD_AREA ->
            getString(Res.string.validation_e03_invalid_area, rodNumber)
        ValidationIssueType.INVALID_ROD_MODULUS ->
            getString(Res.string.validation_e04_invalid_modulus, rodNumber)
        ValidationIssueType.INVALID_ROD_ALLOWABLE_STRESS ->
            getString(Res.string.validation_e05_invalid_allowable_stress, rodNumber)
        ValidationIssueType.NO_SUPPORTS ->
            getString(Res.string.validation_e06_no_supports)
        ValidationIssueType.SUPPORT_INDEX_OUT_OF_RANGE ->
            getString(Res.string.validation_e07_support_out_of_range, nodeIndex ?: 0, maxNodeIndex)
        ValidationIssueType.NON_FINITE_NUMBER ->
            if (rodIndex != null) {
                getString(Res.string.validation_e08_nan_in_rod, rodNumber)
            } else {
                getString(Res.string.validation_e08_nan_in_node, nodeNumber)
            }
        ValidationIssueType.FORCES_SIZE_MISMATCH ->
            getString(
                Res.string.validation_e09_forces_size_mismatch,
                actualForcesCount ?: 0,
                expectedNodesCount ?: 0,
            )
        ValidationIssueType.NO_LOADS ->
            getString(Res.string.validation_w01_no_loads)
        ValidationIssueType.FORCE_AT_SUPPORT ->
            getString(Res.string.validation_w02_force_at_support, nodeNumber)
    }
}
