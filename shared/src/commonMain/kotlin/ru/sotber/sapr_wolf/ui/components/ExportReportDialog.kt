package ru.sotber.sapr_wolf.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.postprocessor.ReportFormat
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.dialog_export_report_cancel
import sapr_wolf.shared.generated.resources.dialog_export_report_html
import sapr_wolf.shared.generated.resources.dialog_export_report_message
import sapr_wolf.shared.generated.resources.dialog_export_report_title
import sapr_wolf.shared.generated.resources.dialog_export_report_txt

@Composable
fun ExportReportDialog(
    onSelectFormat: (ReportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.dialog_export_report_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
            ) {
                Text(
                    text = stringResource(Res.string.dialog_export_report_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = { onSelectFormat(ReportFormat.Html) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(Res.string.dialog_export_report_html))
                    }

                    OutlinedButton(
                        onClick = { onSelectFormat(ReportFormat.Text) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(Res.string.dialog_export_report_txt))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_export_report_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
