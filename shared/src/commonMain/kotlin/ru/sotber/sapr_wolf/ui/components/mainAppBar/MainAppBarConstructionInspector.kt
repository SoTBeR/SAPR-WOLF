package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.ui.theme.CapsuleShape
import ru.sotber.sapr_wolf.ui.theme.FieldShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoTextStyle
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.ic_back
import sapr_wolf.shared.generated.resources.ic_close
import sapr_wolf.shared.generated.resources.ic_forward
import sapr_wolf.shared.generated.resources.inspector_add_rod
import sapr_wolf.shared.generated.resources.inspector_add_rod_left
import sapr_wolf.shared.generated.resources.inspector_add_rod_right
import sapr_wolf.shared.generated.resources.inspector_delete_rod
import sapr_wolf.shared.generated.resources.inspector_field_allowable_stress
import sapr_wolf.shared.generated.resources.inspector_field_area
import sapr_wolf.shared.generated.resources.inspector_field_distributed_load
import sapr_wolf.shared.generated.resources.inspector_field_length
import sapr_wolf.shared.generated.resources.inspector_field_modulus
import sapr_wolf.shared.generated.resources.inspector_field_node_force
import sapr_wolf.shared.generated.resources.inspector_field_node_support
import sapr_wolf.shared.generated.resources.inspector_node_title
import sapr_wolf.shared.generated.resources.inspector_rod_title
import sapr_wolf.shared.generated.resources.inspector_tab_node
import sapr_wolf.shared.generated.resources.inspector_tab_rod

enum class InspectorTab {
    Rod,
    Node,
}

@Composable
fun MainAppBarInspectorHeader(
    activeTab: InspectorTab,
    title: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onSelectTab: (InspectorTab) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val compactButtonPadding = PaddingValues(
        horizontal = MaterialThemeSpacing.small,
        vertical = MaterialThemeSpacing.extraSmall,
    )

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MainAppBarIconButton(
            iconRes = Res.drawable.ic_close,
            onClick = onClose,
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CapsuleShape,
            )
            .padding(2.dp),
    ) {
        MainAppBarButton(
            textRes = Res.string.inspector_tab_rod,
            isSelected = activeTab == InspectorTab.Rod,
            style = MainAppBarButtonStyle.Segment,
            textStyle = MaterialTheme.typography.labelMediumBold,
            contentPadding = compactButtonPadding,
            onClick = { onSelectTab(InspectorTab.Rod) },
            modifier = Modifier.weight(1f),
        )

        MainAppBarButton(
            textRes = Res.string.inspector_tab_node,
            isSelected = activeTab == InspectorTab.Node,
            style = MainAppBarButtonStyle.Segment,
            textStyle = MaterialTheme.typography.labelMediumBold,
            contentPadding = compactButtonPadding,
            onClick = { onSelectTab(InspectorTab.Node) },
            modifier = Modifier.weight(1f),
        )
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MainAppBarIconButton(
            iconRes = Res.drawable.ic_back,
            enabled = canGoPrevious,
            onClick = onPrevious,
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelMediumBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MaterialThemeSpacing.extraSmall),
        )

        MainAppBarIconButton(
            iconRes = Res.drawable.ic_forward,
            enabled = canGoNext,
            onClick = onNext,
        )
    }
}

@Composable
fun MainAppBarInspectorFieldRow(
    modifier: Modifier = Modifier,
    labelRes: StringResource,
    value: Double,
    elementKey: Any,
    onValueChange: (Double) -> Unit,
    allowNegative: Boolean = false,
    allowZero: Boolean = false,
) {
    MainAppBarInspectorFieldRow(
        label = stringResource(labelRes),
        value = value,
        elementKey = elementKey,
        onValueChange = onValueChange,
        allowNegative = allowNegative,
        allowZero = allowZero,
        modifier = modifier,
    )
}

@Composable
fun MainAppBarInspectorFieldRow(
    modifier: Modifier = Modifier,
    label: String,
    value: Double,
    elementKey: Any,
    onValueChange: (Double) -> Unit,
    allowNegative: Boolean = false,
    allowZero: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    var text by remember(elementKey) { mutableStateOf(value.toString()) }
    var isFocused by remember(elementKey) { mutableStateOf(false) }

    LaunchedEffect(elementKey, value) {
        text = value.toString()
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .weight(1f)
                .clip(FieldShape)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = FieldShape,
                )
                .border(
                    width = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outline,
                    shape = FieldShape,
                )
                .padding(
                    horizontal = MaterialThemeSpacing.small,
                    vertical = 5.dp,
                ),
        ) {
            BasicTextField(
                value = text,
                onValueChange = { rawInput ->
                    text = rawInput.filterIndexed { index, ch ->
                        ch.isDigit() ||
                            ch == '.' ||
                            ch == ',' ||
                            ch == 'e' ||
                            ch == 'E' ||
                            (ch == '-' && (allowNegative || (index > 0 && (rawInput[index - 1] == 'e' || rawInput[index - 1] == 'E'))))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                textStyle = MonoTextStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                            focusManager.clearFocus()
                            true
                        } else {
                            false
                        }
                    }
                    .onFocusChanged { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            val currentParsed = text.replace(',', '.').toDoubleOrNull()
                            val isValid = currentParsed != null && currentParsed.isFinite() && when {
                                !allowNegative && !allowZero -> currentParsed > 0.0
                                !allowNegative && allowZero -> currentParsed >= 0.0
                                !allowZero -> currentParsed != 0.0
                                else -> true
                            }
                            if (isValid) {
                                if (currentParsed != value) {
                                    onValueChange(currentParsed)
                                }
                                text = currentParsed.toString()
                            } else {
                                text = value.toString()
                            }
                        }
                        isFocused = focusState.isFocused
                    },
            )
        }
    }
}

@Composable
fun MainAppBarRodInspector(
    hazeState: HazeState?,
    rodIndex: Int,
    rodsCount: Int,
    rod: Rod,
    onSelectRod: (Int) -> Unit,
    onSelectNodeTab: () -> Unit,
    onUpdateRod: (Rod) -> Unit,
    onInsertRodLeft: () -> Unit,
    onInsertRodRight: () -> Unit,
    onDeleteRod: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compactButtonPadding = PaddingValues(
        horizontal = MaterialThemeSpacing.small,
        vertical = MaterialThemeSpacing.extraSmall,
    )

    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        MainAppBarInspectorHeader(
            activeTab = InspectorTab.Rod,
            title = stringResource(Res.string.inspector_rod_title, rodIndex + 1),
            canGoPrevious = rodIndex > 0,
            canGoNext = rodIndex < rodsCount - 1,
            onSelectTab = { tab ->
                if (tab == InspectorTab.Node) {
                    onSelectNodeTab()
                }
            },
            onPrevious = {
                if (rodIndex > 0) {
                    onSelectRod(rodIndex - 1)
                }
            },
            onNext = {
                if (rodIndex < rodsCount - 1) {
                    onSelectRod(rodIndex + 1)
                }
            },
            onClose = onClose,
        )

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MainAppBarInspectorFieldRow(
                labelRes = Res.string.inspector_field_length,
                value = rod.length,
                elementKey = rodIndex,
                onValueChange = { newLength ->
                    onUpdateRod(rod.copy(length = newLength))
                },
            )
            MainAppBarInspectorFieldRow(
                labelRes = Res.string.inspector_field_area,
                value = rod.crossSectionArea,
                elementKey = rodIndex,
                onValueChange = { newArea ->
                    onUpdateRod(rod.copy(crossSectionArea = newArea))
                },
            )
            MainAppBarInspectorFieldRow(
                labelRes = Res.string.inspector_field_modulus,
                value = rod.elasticModulus,
                elementKey = rodIndex,
                onValueChange = { newModulus ->
                    onUpdateRod(rod.copy(elasticModulus = newModulus))
                },
            )
            MainAppBarInspectorFieldRow(
                labelRes = Res.string.inspector_field_allowable_stress,
                value = rod.allowableStress,
                elementKey = rodIndex,
                onValueChange = { newAllowableStress ->
                    onUpdateRod(rod.copy(allowableStress = newAllowableStress))
                },
            )
            MainAppBarInspectorFieldRow(
                labelRes = Res.string.inspector_field_distributed_load,
                value = rod.distributedLoad,
                elementKey = rodIndex,
                allowNegative = true,
                allowZero = true,
                onValueChange = { newLoad ->
                    onUpdateRod(rod.copy(distributedLoad = newLoad))
                },
            )
        }

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.inspector_add_rod),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MainAppBarButton(
                    textRes = Res.string.inspector_add_rod_left,
                    style = MainAppBarButtonStyle.Tonal,
                    shape = FieldShape,
                    textStyle = MaterialTheme.typography.labelMediumBold,
                    contentPadding = compactButtonPadding,
                    onClick = onInsertRodLeft,
                    modifier = Modifier.weight(1f),
                )

                MainAppBarButton(
                    textRes = Res.string.inspector_add_rod_right,
                    style = MainAppBarButtonStyle.Tonal,
                    shape = FieldShape,
                    textStyle = MaterialTheme.typography.labelMediumBold,
                    contentPadding = compactButtonPadding,
                    onClick = onInsertRodRight,
                    modifier = Modifier.weight(1f),
                )
            }

            MainAppBarButton(
                textRes = Res.string.inspector_delete_rod,
                style = MainAppBarButtonStyle.DestructiveTonal,
                shape = FieldShape,
                textStyle = MaterialTheme.typography.labelMediumBold,
                contentPadding = compactButtonPadding,
                onClick = onDeleteRod,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun MainAppBarNodeInspector(
    hazeState: HazeState?,
    nodeIndex: Int,
    nodesCount: Int,
    nodeForce: Double,
    isNodeFixed: Boolean,
    onSelectNode: (Int) -> Unit,
    onSelectRodTab: () -> Unit,
    onUpdateNodeForce: (Double) -> Unit,
    onToggleNodeFixed: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        MainAppBarInspectorHeader(
            activeTab = InspectorTab.Node,
            title = stringResource(Res.string.inspector_node_title, nodeIndex + 1),
            canGoPrevious = nodeIndex > 0,
            canGoNext = nodeIndex < nodesCount - 1,
            onSelectTab = { tab ->
                if (tab == InspectorTab.Rod) {
                    onSelectRodTab()
                }
            },
            onPrevious = {
                if (nodeIndex > 0) {
                    onSelectNode(nodeIndex - 1)
                }
            },
            onNext = {
                if (nodeIndex < nodesCount - 1) {
                    onSelectNode(nodeIndex + 1)
                }
            },
            onClose = onClose,
        )

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MainAppBarInspectorFieldRow(
                labelRes = Res.string.inspector_field_node_force,
                value = nodeForce,
                elementKey = nodeIndex,
                allowNegative = true,
                allowZero = true,
                onValueChange = onUpdateNodeForce,
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.inspector_field_node_support),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Switch(
                    checked = isNodeFixed,
                    onCheckedChange = { onToggleNodeFixed() },
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.75f
                            scaleY = 0.75f
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        },
                )
            }
        }
    }
}
