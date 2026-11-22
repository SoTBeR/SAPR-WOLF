package ru.sotber.sapr_wolf.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.preprocessor.CalculationInvalidationReason
import ru.sotber.sapr_wolf.preprocessor.localizedMessage
import ru.sotber.sapr_wolf.ui.FileDialogAlert
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import sapr_wolf.shared.generated.resources.*

@Composable
fun ProjectFileAlertDialog(
    alert: FileDialogAlert,
    onDismiss: () -> Unit,
    onConfirmLegacyJsonConversion: () -> Unit = {},
    onCancelLegacyJsonConversion: () -> Unit = {},
) {
    val isMandatoryConfirmation = alert is FileDialogAlert.ConfirmLegacyJsonConversion

    val title = when (alert) {
        is FileDialogAlert.NormalizedOnLoad -> stringResource(Res.string.dialog_normalized_title)
        is FileDialogAlert.CalculationInvalidated -> stringResource(Res.string.dialog_calculation_invalidated_title)
        is FileDialogAlert.ConfirmLegacyJsonConversion -> stringResource(Res.string.dialog_legacy_conversion_title)
        is FileDialogAlert.LoadError -> stringResource(Res.string.dialog_load_error_title)
        is FileDialogAlert.SaveError -> stringResource(Res.string.dialog_save_error_title)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isMandatoryConfirmation) {
                onDismiss()
            }
        },
        properties = if (isMandatoryConfirmation) {
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            )
        } else {
            DialogProperties()
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (alert) {
                    is FileDialogAlert.NormalizedOnLoad -> {
                        Text(
                            text = stringResource(Res.string.dialog_normalized_header),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                        ) {
                            for (change in alert.changes) {
                                Text(
                                    text = change.localizedMessage(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is FileDialogAlert.CalculationInvalidated -> {
                        val reasonMessage = when (alert.reason) {
                            CalculationInvalidationReason.HashMismatch ->
                                stringResource(Res.string.dialog_calculation_invalidated_tampered)
                            CalculationInvalidationReason.MathVerificationFailed ->
                                stringResource(Res.string.dialog_calculation_invalidated_math)
                            CalculationInvalidationReason.OutdatedSolverVersion ->
                                stringResource(Res.string.dialog_calculation_invalidated_outdated)
                            CalculationInvalidationReason.NormalizedConstruction ->
                                stringResource(Res.string.dialog_calculation_invalidated_normalized)
                        }
                        Text(
                            text = reasonMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (alert.normalizationChanges.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                            ) {
                                for (change in alert.normalizationChanges) {
                                    Text(
                                        text = change.localizedMessage(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    is FileDialogAlert.ConfirmLegacyJsonConversion -> {
                        Text(
                            text = stringResource(Res.string.dialog_legacy_conversion_message, alert.projectName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    is FileDialogAlert.LoadError -> {
                        Text(
                            text = stringResource(Res.string.dialog_load_error_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    is FileDialogAlert.SaveError -> {
                        Text(
                            text = stringResource(Res.string.dialog_save_error_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (alert) {
                is FileDialogAlert.ConfirmLegacyJsonConversion -> {
                    Button(onClick = onConfirmLegacyJsonConversion) {
                        Text(text = stringResource(Res.string.dialog_yes))
                    }
                }

                else -> {
                    Button(onClick = onDismiss) {
                        Text(text = stringResource(Res.string.dialog_continue))
                    }
                }
            }
        },
        dismissButton = if (alert is FileDialogAlert.ConfirmLegacyJsonConversion) {
            {
                TextButton(onClick = onCancelLegacyJsonConversion) {
                    Text(text = stringResource(Res.string.dialog_no))
                }
            }
        } else {
            null
        },
        containerColor = MaterialThemeExtraColors.glassDialog,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
