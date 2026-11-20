package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.RodStrength
import ru.sotber.sapr_wolf.postprocessor.PostprocessorSummary
import ru.sotber.sapr_wolf.postprocessor.calculateSection
import ru.sotber.sapr_wolf.postprocessor.summarizePostprocessor
import ru.sotber.sapr_wolf.ui.CalculationState
import ru.sotber.sapr_wolf.ui.components.EpureComponent
import ru.sotber.sapr_wolf.ui.components.EpureDisplayMode
import ru.sotber.sapr_wolf.ui.components.PlotCanvas
import ru.sotber.sapr_wolf.ui.formatTableValue
import ru.sotber.sapr_wolf.ui.theme.CapsuleShape
import ru.sotber.sapr_wolf.ui.theme.FieldShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoTextStyle
import ru.sotber.sapr_wolf.ui.theme.Shapes
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold
import sapr_wolf.shared.generated.resources.*
import kotlin.math.abs

private enum class PostprocessorViewTab {
    Table,
    Plots,
}

private object PostprocessorPanelDefaults {
    val maxTableHeight = 190.dp
    val minTableContentWidth = 540.dp
    val plotChartHeight = 104.dp
    const val rodColWeight = 1.05f
    const val xColWeight = 0.85f
    const val valueColWeight = 1.05f
    const val stressMaxColWeight = 1.0f
    const val safetyColWeight = 1.0f
}

@Composable
fun MainAppBarEpureSelector(
    hazeState: HazeState,
    selectedMode: EpureDisplayMode,
    onSelectMode: (EpureDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compactPadding = PaddingValues(
        horizontal = MaterialThemeSpacing.small,
        vertical = MaterialThemeSpacing.extraSmall,
    )

    MainAppBarRow(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        MainAppBarButton(
            textRes = Res.string.postprocessor_epure_all,
            style = MainAppBarButtonStyle.Segment,
            isSelected = selectedMode == EpureDisplayMode.All,
            contentPadding = compactPadding,
            onClick = { onSelectMode(EpureDisplayMode.All) },
        )

        MainAppBarButton(
            text = EpureComponent.AxialForce.symbol,
            style = MainAppBarButtonStyle.Segment,
            isSelected = selectedMode == EpureDisplayMode.AxialForce,
            contentPadding = compactPadding,
            onClick = { onSelectMode(EpureDisplayMode.AxialForce) },
        )

        MainAppBarButton(
            text = EpureComponent.NormalStress.symbol,
            style = MainAppBarButtonStyle.Segment,
            isSelected = selectedMode == EpureDisplayMode.NormalStress,
            contentPadding = compactPadding,
            onClick = { onSelectMode(EpureDisplayMode.NormalStress) },
        )

        MainAppBarButton(
            text = EpureComponent.Displacement.symbol,
            style = MainAppBarButtonStyle.Segment,
            isSelected = selectedMode == EpureDisplayMode.Displacement,
            contentPadding = compactPadding,
            onClick = { onSelectMode(EpureDisplayMode.Displacement) },
        )

        MainAppBarButton(
            textRes = Res.string.postprocessor_epure_none,
            style = MainAppBarButtonStyle.Segment,
            isSelected = selectedMode == EpureDisplayMode.None,
            contentPadding = compactPadding,
            onClick = { onSelectMode(EpureDisplayMode.None) },
        )
    }
}

@Composable
fun MainAppBarPostprocessorResultsTable(
    hazeState: HazeState?,
    construction: Construction,
    result: CalculationState.Success,
    selectedRodIndex: Int?,
    inspectedRodIndex: Int?,
    inspectedLocalX: Double,
    onInspectSection: (Int, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (construction.rods.isEmpty()) return

    val displacementsArray = remember(result.displacements) {
        result.displacements.toDoubleArray()
    }
    val activeRodIndex = (inspectedRodIndex ?: selectedRodIndex ?: 0)
        .coerceIn(0, construction.rods.lastIndex)

    var activeTab by remember { mutableStateOf(PostprocessorViewTab.Table) }

    val compactPadding = PaddingValues(
        horizontal = MaterialThemeSpacing.small,
        vertical = MaterialThemeSpacing.extraSmall,
    )

    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CapsuleShape,
                    )
                    .padding(2.dp),
            ) {
                MainAppBarButton(
                    textRes = Res.string.postprocessor_tab_table,
                    style = MainAppBarButtonStyle.Segment,
                    isSelected = activeTab == PostprocessorViewTab.Table,
                    contentPadding = compactPadding,
                    onClick = { activeTab = PostprocessorViewTab.Table },
                )
                MainAppBarButton(
                    textRes = Res.string.postprocessor_tab_plots,
                    style = MainAppBarButtonStyle.Segment,
                    isSelected = activeTab == PostprocessorViewTab.Plots,
                    contentPadding = compactPadding,
                    onClick = { activeTab = PostprocessorViewTab.Plots },
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = CapsuleShape,
                    )
                    .padding(
                        horizontal = MaterialThemeSpacing.small,
                        vertical = MaterialThemeSpacing.extraSmall,
                    ),
            ) {
                Text(
                    text = construction.rods.size.toString(),
                    style = MonoTextStyle.copy(color = MaterialTheme.colorScheme.primary),
                    maxLines = 1,
                )
            }
        }

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        when (activeTab) {
            PostprocessorViewTab.Table -> {
                PostprocessorRodsTable(
                    construction = construction,
                    displacementsArray = displacementsArray,
                    rodStrengths = result.rodStrengths,
                    activeRodIndex = activeRodIndex,
                    onInspectSection = onInspectSection,
                )
            }

            PostprocessorViewTab.Plots -> {
                PostprocessorPlotsContent(
                    construction = construction,
                    displacementsArray = displacementsArray,
                    activeRodIndex = activeRodIndex,
                    inspectedLocalX = inspectedLocalX,
                    onSelectRod = { rodIdx -> onInspectSection(rodIdx, 0.0) },
                )
            }
        }
    }
}

@Composable
fun MainAppBarPostprocessorSectionPanel(
    hazeState: HazeState?,
    construction: Construction,
    result: CalculationState.Success,
    selectedRodIndex: Int?,
    inspectedRodIndex: Int?,
    inspectedLocalX: Double,
    onInspectSection: (Int, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (construction.rods.isEmpty()) return

    val displacementsArray = remember(result.displacements) {
        result.displacements.toDoubleArray()
    }
    val summary = remember(construction, displacementsArray, result.rodStrengths) {
        summarizePostprocessor(construction, displacementsArray, result.rodStrengths)
    }
    val activeRodIndex = (inspectedRodIndex ?: selectedRodIndex ?: 0)
        .coerceIn(0, construction.rods.lastIndex)

    PostprocessorSectionAndStrengthPanel(
        hazeState = hazeState,
        construction = construction,
        result = result,
        displacementsArray = displacementsArray,
        summary = summary,
        activeRodIndex = activeRodIndex,
        inspectedLocalX = inspectedLocalX,
        onInspectSection = onInspectSection,
        modifier = modifier,
    )
}

@Composable
private fun PostprocessorRodsTable(
    construction: Construction,
    displacementsArray: DoubleArray,
    rodStrengths: List<RodStrength>,
    activeRodIndex: Int,
    onInspectSection: (Int, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val extraColors = MaterialThemeExtraColors
    val headerStyle = MonoTextStyle.copy(
        fontWeight = FontWeight.SemiBold,
        color = colorScheme.onSurface,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
            modifier = Modifier.width(PostprocessorPanelDefaults.minTableContentWidth),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.small)
                    .background(extraColors.glassChip)
                    .padding(horizontal = MaterialThemeSpacing.small, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.postprocessor_col_rod),
                    style = headerStyle,
                    maxLines = 1,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.rodColWeight),
                )
                Text(
                    text = "x",
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.xColWeight),
                )
                Text(
                    text = "Nx",
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                )
                Text(
                    text = "σx",
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                )
                Text(
                    text = "Ux",
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                )
                Text(
                    text = "σmax",
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.stressMaxColWeight),
                )
                Text(
                    text = "n",
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.safetyColWeight),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PostprocessorPanelDefaults.maxTableHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                construction.rods.forEachIndexed { rodIndex, rod ->
                    if (rodIndex > 0) {
                        HorizontalDivider(
                            thickness = DividerDefaults.Thickness,
                            color = colorScheme.outline.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }

                    val strength = rodStrengths.getOrNull(rodIndex)
                    val xPoints = listOf(0.0, rod.length)

                    xPoints.forEachIndexed { pointIdx, localX ->
                        val section = calculateSection(construction, displacementsArray, rodIndex, localX)
                        val isOverloaded = strength != null && !strength.isSatisfied
                        val isSelectedRod = activeRodIndex == rodIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()

                        val rowBg = when {
                            isOverloaded -> colorScheme.error.copy(alpha = if (isSelectedRod) 0.18f else 0.10f)
                            isSelectedRod -> extraColors.glassChip
                            isHovered -> colorScheme.surfaceVariant
                            else -> Color.Transparent
                        }
                        val rowBorder = when {
                            isOverloaded && isSelectedRod -> colorScheme.error.copy(alpha = 0.65f)
                            isSelectedRod -> colorScheme.primary.copy(alpha = 0.55f)
                            else -> Color.Transparent
                        }
                        val textColor = if (isOverloaded) colorScheme.error else colorScheme.onSurface
                        val mutedColor = if (isOverloaded) {
                            colorScheme.error.copy(alpha = 0.80f)
                        } else {
                            colorScheme.onSurfaceVariant
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.small)
                                .background(rowBg)
                                .border(width = 1.dp, color = rowBorder, shape = Shapes.small)
                                .hoverable(interactionSource)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onInspectSection(rodIndex, localX) },
                                )
                                .padding(horizontal = MaterialThemeSpacing.small, vertical = 5.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.rodColWeight),
                            ) {
                                if (pointIdx == 0) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                color = when {
                                                    isOverloaded -> colorScheme.error
                                                    isSelectedRod -> colorScheme.primary
                                                    else -> extraColors.glassChip
                                                },
                                                shape = Shapes.extraSmall,
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "${rodIndex + 1}",
                                            style = MonoTextStyle.copy(
                                                color = when {
                                                    isOverloaded -> colorScheme.onError
                                                    isSelectedRod -> colorScheme.onPrimary
                                                    else -> colorScheme.onSurface
                                                },
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }

                            Text(
                                text = formatTableValue(localX),
                                style = MonoTextStyle.copy(color = mutedColor),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.xColWeight),
                            )
                            Text(
                                text = formatTableValue(section.axialForce),
                                style = MonoTextStyle.copy(color = textColor),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                            )
                            Text(
                                text = formatTableValue(section.normalStress),
                                style = MonoTextStyle.copy(color = textColor),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                            )
                            Text(
                                text = formatTableValue(section.displacement),
                                style = MonoTextStyle.copy(color = textColor),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                            )
                            Text(
                                text = if (pointIdx == 0 && strength != null) formatTableValue(strength.maxStress) else "",
                                style = MonoTextStyle.copy(color = textColor),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.stressMaxColWeight),
                            )
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier.weight(PostprocessorPanelDefaults.safetyColWeight),
                            ) {
                                if (pointIdx == 0 && strength != null) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(
                                                color = if (strength.isSatisfied) {
                                                    colorScheme.primary.copy(alpha = 0.12f)
                                                } else {
                                                    colorScheme.error.copy(alpha = 0.18f)
                                                },
                                                shape = CapsuleShape,
                                            )
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            text = formatTableValue(strength.safetyFactor),
                                            style = MonoTextStyle.copy(
                                                color = if (strength.isSatisfied) colorScheme.primary else colorScheme.error,
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostprocessorPlotsContent(
    construction: Construction,
    displacementsArray: DoubleArray,
    activeRodIndex: Int,
    inspectedLocalX: Double,
    onSelectRod: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MainAppBarIconButton(
                iconRes = Res.drawable.ic_back,
                enabled = activeRodIndex > 0,
                onClick = { onSelectRod(activeRodIndex - 1) },
            )
            Text(
                text = stringResource(Res.string.inspector_rod_title, activeRodIndex + 1),
                style = MaterialTheme.typography.labelMediumBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MainAppBarIconButton(
                iconRes = Res.drawable.ic_forward,
                enabled = activeRodIndex < construction.rods.lastIndex,
                onClick = { onSelectRod(activeRodIndex + 1) },
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = PostprocessorPanelDefaults.maxTableHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            PlotCanvas(
                construction = construction,
                displacements = displacementsArray,
                rodIndex = activeRodIndex,
                component = EpureComponent.AxialForce,
                inspectedLocalX = inspectedLocalX,
                chartHeight = PostprocessorPanelDefaults.plotChartHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            PlotCanvas(
                construction = construction,
                displacements = displacementsArray,
                rodIndex = activeRodIndex,
                component = EpureComponent.NormalStress,
                inspectedLocalX = inspectedLocalX,
                chartHeight = PostprocessorPanelDefaults.plotChartHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            PlotCanvas(
                construction = construction,
                displacements = displacementsArray,
                rodIndex = activeRodIndex,
                component = EpureComponent.Displacement,
                inspectedLocalX = inspectedLocalX,
                chartHeight = PostprocessorPanelDefaults.plotChartHeight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PostprocessorSectionAndStrengthPanel(
    hazeState: HazeState?,
    construction: Construction,
    result: CalculationState.Success,
    displacementsArray: DoubleArray,
    summary: PostprocessorSummary,
    activeRodIndex: Int,
    inspectedLocalX: Double,
    onInspectSection: (Int, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rod = construction.rods[activeRodIndex]
    val clampedX = inspectedLocalX.coerceIn(0.0, rod.length)
    val section = remember(construction, displacementsArray, activeRodIndex, clampedX) {
        calculateSection(construction, displacementsArray, activeRodIndex, clampedX)
    }
    val rodStrength = result.rodStrengths.getOrNull(activeRodIndex)

    var xText by remember(activeRodIndex, clampedX) {
        mutableStateOf(formatTableValue(clampedX))
    }
    LaunchedEffect(activeRodIndex, clampedX) {
        val parsed = xText.replace(',', '.').toDoubleOrNull()
        if (parsed == null || abs(parsed - clampedX) > 1e-6) {
            xText = formatTableValue(clampedX)
        }
    }

    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.postprocessor_section_title),
                style = MaterialTheme.typography.labelMediumBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainAppBarIconButton(
                    iconRes = Res.drawable.ic_back,
                    enabled = activeRodIndex > 0,
                    onClick = { onInspectSection(activeRodIndex - 1, 0.0) },
                )
                Text(
                    text = stringResource(Res.string.inspector_rod_title, activeRodIndex + 1),
                    style = MonoTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MainAppBarIconButton(
                    iconRes = Res.drawable.ic_forward,
                    enabled = activeRodIndex < construction.rods.lastIndex,
                    onClick = { onInspectSection(activeRodIndex + 1, 0.0) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "x ∈ [0, ${formatTableValue(rod.length)}]",
                style = MonoTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f),
            )
            CompactNumericField(
                value = xText,
                onValueChange = { next ->
                    xText = next
                    next.replace(',', '.').toDoubleOrNull()?.let { parsed ->
                        onInspectSection(activeRodIndex, parsed.coerceIn(0.0, rod.length))
                    }
                },
                modifier = Modifier.width(88.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MetricChip(label = "Nx", value = formatTableValue(section.axialForce), modifier = Modifier.weight(1f))
            MetricChip(
                label = "σx",
                value = formatTableValue(section.normalStress),
                isError = rodStrength != null && abs(section.normalStress) > rodStrength.allowableStress + 1e-9,
                modifier = Modifier.weight(1f),
            )
            MetricChip(label = "Ux", value = formatTableValue(section.displacement), modifier = Modifier.weight(1f))
        }

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        val hasOverload = summary.overloadedRodIndices.isNotEmpty()
        val colorScheme = MaterialTheme.colorScheme
        val statusColor = if (hasOverload) colorScheme.error else colorScheme.primary
        val statusBg = if (hasOverload) colorScheme.error.copy(alpha = 0.14f) else colorScheme.primary.copy(alpha = 0.12f)
        val statusText = if (hasOverload) {
            stringResource(
                Res.string.postprocessor_strength_overload,
                summary.overloadedRodIndices.joinToString(", ") { "${it + 1}" },
            )
        } else {
            stringResource(Res.string.postprocessor_strength_ok)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.small)
                .background(statusBg)
                .padding(horizontal = MaterialThemeSpacing.small, vertical = 6.dp),
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMediumBold,
                color = statusColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        MostLoadedRodCard(
            mostLoadedRodIndex = summary.mostLoadedRodIndex,
            minSafetyFactor = summary.minSafetyFactor,
            hasOverload = hasOverload,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PeakMetricCard(
                label = "max |Nx|",
                value = formatTableValue(summary.maxAbsAxialForce),
                modifier = Modifier.weight(1f),
            )
            PeakMetricCard(
                label = "max |σx|",
                value = formatTableValue(summary.maxAbsNormalStress),
                modifier = Modifier.weight(1f),
            )
            PeakMetricCard(
                label = "max |Ux|",
                value = formatTableValue(summary.maxAbsDisplacement),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MostLoadedRodCard(
    mostLoadedRodIndex: Int?,
    minSafetyFactor: Double,
    hasOverload: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (hasOverload) colorScheme.error.copy(alpha = 0.5f) else colorScheme.outlineVariant
    val accentColor = if (hasOverload) colorScheme.error else colorScheme.primary

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.small)
            .background(colorScheme.surfaceVariant)
            .border(width = 1.dp, color = borderColor, shape = Shapes.small)
            .padding(horizontal = MaterialThemeSpacing.mediumSmall, vertical = MaterialThemeSpacing.small),
    ) {
        Text(
            text = stringResource(Res.string.postprocessor_most_loaded),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = mostLoadedRodIndex?.let {
                    stringResource(Res.string.inspector_rod_title, it + 1)
                } ?: "—",
                style = MaterialTheme.typography.labelMediumBold,
                color = if (hasOverload) colorScheme.error else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (mostLoadedRodIndex != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = CapsuleShape,
                        )
                        .padding(horizontal = MaterialThemeSpacing.small, vertical = 2.dp),
                ) {
                    Text(
                        text = "n=${formatTableValue(minSafetyFactor)}",
                        style = MonoTextStyle.copy(color = accentColor),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeakMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(Shapes.small)
            .background(colorScheme.surfaceVariant)
            .border(width = 1.dp, color = colorScheme.outlineVariant, shape = Shapes.small)
            .padding(horizontal = MaterialThemeSpacing.extraSmall, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MonoTextStyle.copy(
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = if (isError) colorScheme.error.copy(alpha = 0.16f) else MaterialThemeExtraColors.glassChip
    val valueColor = if (isError) colorScheme.error else colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(Shapes.small)
            .background(bgColor)
            .padding(horizontal = MaterialThemeSpacing.extraSmall, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MonoTextStyle.copy(color = colorScheme.onSurfaceVariant),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MonoTextStyle.copy(color = valueColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MonoTextStyle.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() },
        ),
        modifier = modifier
            .clip(FieldShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = FieldShape,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    focusManager.clearFocus()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}
