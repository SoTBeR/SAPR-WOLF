package ru.sotber.sapr_wolf.ui.components.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.ui.modifiers.pointerCursor
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoSmallTextStyle
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold
import sapr_wolf.shared.generated.resources.*

private enum class HelpTab(val titleRes: StringResource) {
    Controls(Res.string.dialog_help_tab_controls),
    Notation(Res.string.dialog_help_tab_notation),
    About(Res.string.dialog_help_tab_about),
}

private data class HelpEntry(
    val keyRes: StringResource,
    val descriptionRes: StringResource,
    val suffixRes: StringResource? = null,
)

@Composable
fun HelpDialog(
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HelpTab.Controls) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialThemeExtraColors.glassDialog,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            modifier = Modifier
                .padding(MaterialThemeSpacing.medium)
                .widthIn(min = 320.dp, max = 580.dp)
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F1) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
        ) {
            Column(
                modifier = Modifier.padding(MaterialThemeSpacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(Res.string.dialog_help_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .pointerCursor(),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.dialog_help_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                HelpTabSelector(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                )

                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState),
                ) {
                    when (selectedTab) {
                        HelpTab.Controls -> HelpControlsContent()
                        HelpTab.Notation -> HelpNotationContent()
                        HelpTab.About -> HelpAboutContent()
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.pointerCursor(),
                    ) {
                        Text(text = stringResource(Res.string.dialog_help_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpTabSelector(
    selectedTab: HelpTab,
    onSelectTab: (HelpTab) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(MaterialThemeSpacing.extraSmall),
    ) {
        HelpTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        },
                    )
                    .pointerCursor()
                    .clickable { onSelectTab(tab) }
                    .padding(
                        horizontal = MaterialThemeSpacing.small,
                        vertical = MaterialThemeSpacing.small,
                    ),
            ) {
                Text(
                    text = stringResource(tab.titleRes),
                    style = if (isSelected) {
                        MaterialTheme.typography.labelMediumBold
                    } else {
                        MaterialTheme.typography.labelMedium
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HelpControlsContent() {
    val projectEntries = listOf(
        HelpEntry(Res.string.help_key_new_project, Res.string.help_desc_new_project),
        HelpEntry(Res.string.help_key_open_project, Res.string.help_desc_open_project),
        HelpEntry(Res.string.help_key_save_project, Res.string.help_desc_save_project),
        HelpEntry(Res.string.help_key_save_project_as, Res.string.help_desc_save_project_as),
        HelpEntry(Res.string.help_key_export_report, Res.string.help_desc_export_report),
    )
    val editingEntries = listOf(
        HelpEntry(Res.string.help_key_undo, Res.string.help_desc_undo),
        HelpEntry(Res.string.help_key_redo, Res.string.help_desc_redo),
        HelpEntry(Res.string.help_key_add_rod_left, Res.string.help_desc_add_rod_left),
        HelpEntry(Res.string.help_key_add_rod_right, Res.string.help_desc_add_rod_right),
        HelpEntry(Res.string.help_key_delete_rod, Res.string.help_desc_delete_rod),
        HelpEntry(Res.string.help_key_calculate, Res.string.help_desc_calculate),
    )
    val interfaceCanvasEntries = listOf(
        HelpEntry(Res.string.help_key_reset_zoom, Res.string.help_desc_reset_zoom, Res.string.help_desc_horizontal_only),
        HelpEntry(Res.string.help_key_toggle_theme, Res.string.help_desc_toggle_theme),
        HelpEntry(Res.string.help_key_help, Res.string.help_desc_help),
        HelpEntry(Res.string.help_key_mouse_wheel, Res.string.help_desc_mouse_wheel, Res.string.help_desc_horizontal_only),
        HelpEntry(Res.string.help_key_mouse_drag, Res.string.help_desc_mouse_drag),
        HelpEntry(Res.string.help_key_mouse_click, Res.string.help_desc_mouse_click),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        HelpSection(
            titleRes = Res.string.help_section_project,
            entries = projectEntries,
            badgeMinWidthDp = 152,
        )
        HelpSection(
            titleRes = Res.string.help_section_editing,
            entries = editingEntries,
            badgeMinWidthDp = 152,
        )
        HelpSection(
            titleRes = Res.string.help_section_interface_canvas,
            entries = interfaceCanvasEntries,
            badgeMinWidthDp = 152,
        )
    }
}

@Composable
private fun HelpNotationContent() {
    val inputEntries = listOf(
        HelpEntry(Res.string.help_symbol_length, Res.string.help_desc_symbol_length),
        HelpEntry(Res.string.help_symbol_area, Res.string.help_desc_symbol_area),
        HelpEntry(Res.string.help_symbol_modulus, Res.string.help_desc_symbol_modulus),
        HelpEntry(Res.string.help_symbol_allowable_stress, Res.string.help_desc_symbol_allowable_stress),
        HelpEntry(Res.string.help_symbol_distributed_load, Res.string.help_desc_symbol_distributed_load),
        HelpEntry(Res.string.help_symbol_node_force, Res.string.help_desc_symbol_node_force),
    )
    val resultEntries = listOf(
        HelpEntry(Res.string.help_symbol_displacement, Res.string.help_desc_symbol_displacement),
        HelpEntry(Res.string.help_symbol_reaction, Res.string.help_desc_symbol_reaction),
        HelpEntry(Res.string.help_symbol_local_x, Res.string.help_desc_symbol_local_x),
        HelpEntry(Res.string.help_symbol_axial_force, Res.string.help_desc_symbol_axial_force),
        HelpEntry(Res.string.help_symbol_normal_stress, Res.string.help_desc_symbol_normal_stress),
        HelpEntry(Res.string.help_symbol_section_displacement, Res.string.help_desc_symbol_section_displacement),
        HelpEntry(Res.string.help_symbol_max_stress, Res.string.help_desc_symbol_max_stress),
        HelpEntry(Res.string.help_symbol_safety_factor, Res.string.help_desc_symbol_safety_factor),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        HelpSection(
            titleRes = Res.string.help_section_input_params,
            entries = inputEntries,
            badgeMinWidthDp = 76,
        )
        HelpSection(
            titleRes = Res.string.help_section_results_params,
            entries = resultEntries,
            badgeMinWidthDp = 76,
        )
    }
}

@Composable
private fun HelpAboutContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_app_logo),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.help_about_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(MaterialThemeSpacing.mediumSmall),
        ) {
            AboutInfoRow(
                label = stringResource(Res.string.help_about_version_label),
                value = stringResource(Res.string.help_about_version_value),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            AboutInfoRow(
                label = stringResource(Res.string.help_about_author_label),
                value = stringResource(Res.string.help_about_author_value),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            AboutInfoRow(
                label = stringResource(Res.string.help_about_formats_label),
                value = stringResource(Res.string.help_about_formats_value),
            )
        }

        Text(
            text = stringResource(Res.string.powered_by_sapp),
            style = MonoSmallTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HelpSection(
    titleRes: StringResource,
    entries: List<HelpEntry>,
    badgeMinWidthDp: Int,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelMediumBold,
            color = MaterialTheme.colorScheme.primary,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.mediumSmall),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .widthIn(min = badgeMinWidthDp.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(
                                horizontal = MaterialThemeSpacing.small,
                                vertical = MaterialThemeSpacing.extraSmall,
                            ),
                    ) {
                        Text(
                            text = stringResource(entry.keyRes),
                            style = MonoSmallTextStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }

                    val description = if (entry.suffixRes != null) {
                        "${stringResource(entry.descriptionRes)} ${stringResource(entry.suffixRes)}"
                    } else {
                        stringResource(entry.descriptionRes)
                    }

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}
