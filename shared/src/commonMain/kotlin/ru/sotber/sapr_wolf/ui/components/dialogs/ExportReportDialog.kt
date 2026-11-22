package ru.sotber.sapr_wolf.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.postprocessor.ReportFormat
import ru.sotber.sapr_wolf.ui.modifiers.pointerCursor
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import sapr_wolf.shared.generated.resources.*

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
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .pointerCursor(),
                    ) {
                        Text(text = stringResource(Res.string.dialog_export_report_html))
                    }

                    OutlinedButton(
                        onClick = { onSelectFormat(ReportFormat.Text, openAfterExport) },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .pointerCursor(),
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
                        .pointerCursor()
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
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.pointerCursor(),
            ) {
                Text(text = stringResource(Res.string.dialog_export_report_cancel))
            }
        },
        containerColor = MaterialThemeExtraColors.glassDialog,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
