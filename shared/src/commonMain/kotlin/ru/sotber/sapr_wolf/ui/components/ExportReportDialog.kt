package ru.sotber.sapr_wolf.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.postprocessor.ReportFormat
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.dialog_export_report_cancel
import sapr_wolf.shared.generated.resources.dialog_export_report_html
import sapr_wolf.shared.generated.resources.dialog_export_report_message
import sapr_wolf.shared.generated.resources.dialog_export_report_open_after_export
import sapr_wolf.shared.generated.resources.dialog_export_report_title
import sapr_wolf.shared.generated.resources.dialog_export_report_txt

@Composable
fun ExportReportDialog(
    onSelectFormat: (ReportFormat, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var openAfterExport by remember { mutableStateOf(true) }

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
                        onClick = { onSelectFormat(ReportFormat.Html, openAfterExport) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(Res.string.dialog_export_report_html))
                    }

                    OutlinedButton(
                        onClick = { onSelectFormat(ReportFormat.Text, openAfterExport) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(Res.string.dialog_export_report_txt))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(
                            value = openAfterExport,
                            role = Role.Checkbox,
                            onValueChange = { openAfterExport = it },
                        )
                        .padding(vertical = MaterialThemeSpacing.extraSmall),
                ) {
                    Checkbox(
                        checked = openAfterExport,
                        onCheckedChange = null,
                    )
                    Text(
                        text = stringResource(Res.string.dialog_export_report_open_after_export),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_export_report_cancel))
            }
        },
        containerColor = MaterialThemeExtraColors.glassDialog,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
