package ru.sotber.sapr_wolf.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.localizedMessage
import ru.sotber.sapr_wolf.ui.CalculationState
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.calculation_generic_error
import sapr_wolf.shared.generated.resources.dialog_calculation_error_title
import sapr_wolf.shared.generated.resources.dialog_continue

@Composable
fun CalculationErrorDialog(
    error: CalculationState.Error,
    onDismiss: () -> Unit,
) {
    val message = error.validationIssue?.localizedMessage()
        ?: error.message?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.calculation_generic_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.dialog_calculation_error_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_continue))
            }
        },
        containerColor = MaterialThemeExtraColors.glassDialog,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
