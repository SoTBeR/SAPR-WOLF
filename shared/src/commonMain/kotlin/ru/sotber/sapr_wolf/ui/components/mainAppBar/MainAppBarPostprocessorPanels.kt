package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import ru.sotber.sapr_wolf.ui.components.NumericTextField
import ru.sotber.sapr_wolf.ui.components.canvas.EpureComponent
import ru.sotber.sapr_wolf.ui.components.canvas.EpureDisplayMode
import ru.sotber.sapr_wolf.ui.components.canvas.PlotCanvas
import ru.sotber.sapr_wolf.ui.formatTableValue
import ru.sotber.sapr_wolf.ui.modifiers.pointerCursor
import ru.sotber.sapr_wolf.ui.theme.*
import sapr_wolf.shared.generated.resources.*
import kotlin.math.abs

private object PostprocessorPanelDefaults {
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
    onInspectSection: (Int, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (construction.rods.isEmpty()) return

    val displacementsArray = remember(result.displacements) {
        result.displacements.toDoubleArray()
    }
    val activeRodIndex = (inspectedRodIndex ?: selectedRodIndex ?: 0)
        .coerceIn(0, construction.rods.lastIndex)

    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialThemeSpacing.extraSmall),
        ) {
            Text(
                text = stringResource(Res.string.postprocessor_tab_table),
                style = MaterialTheme.typography.labelMediumBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

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

        PostprocessorRodsTable(
            construction = construction,
            displacementsArray = displacementsArray,
            rodStrengths = result.rodStrengths,
            activeRodIndex = activeRodIndex,
            onInspectSection = onInspectSection,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun MainAppBarPostprocessorPlotsPanel(
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

    MainAppBarColumn(
        hazeState = hazeState,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialThemeSpacing.extraSmall),
        ) {
            Text(
                text = stringResource(Res.string.postprocessor_tab_plots),
                style = MaterialTheme.typography.labelMediumBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        PostprocessorPlotsContent(
            construction = construction,
            displacementsArray = displacementsArray,
            activeRodIndex = activeRodIndex,
            inspectedLocalX = inspectedLocalX,
            modifier = Modifier.weight(1f),
        )
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

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val tableWidth = maxOf(maxWidth, PostprocessorPanelDefaults.minTableContentWidth)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
                modifier = Modifier
                    .width(tableWidth)
                    .fillMaxHeight(),
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
                    text = stringResource(Res.string.postprocessor_col_x),
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.xColWeight),
                )
                Text(
                    text = stringResource(Res.string.postprocessor_col_axial_force),
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                )
                Text(
                    text = stringResource(Res.string.postprocessor_col_normal_stress),
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                )
                Text(
                    text = stringResource(Res.string.postprocessor_col_displacement),
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.valueColWeight),
                )
                Text(
                    text = stringResource(Res.string.postprocessor_col_max_stress),
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.stressMaxColWeight),
                )
                Text(
                    text = stringResource(Res.string.postprocessor_col_safety_factor),
                    style = headerStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PostprocessorPanelDefaults.safetyColWeight),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                                .pointerCursor()
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
}

@Composable
private fun PostprocessorPlotsContent(
    construction: Construction,
    displacementsArray: DoubleArray,
    activeRodIndex: Int,
    inspectedLocalX: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
        modifier = modifier
            .fillMaxWidth()
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
                text = stringResource(Res.string.postprocessor_section_range, formatTableValue(rod.length)),
                style = MonoTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f),
            )
            NumericTextField(
                value = clampedX,
                onValueChange = { nextX ->
                    onInspectSection(activeRodIndex, nextX)
                },
                elementKey = activeRodIndex,
                allowNegative = false,
                allowZero = true,
                allowScientificNotation = true,
                minValue = 0.0,
                maxValue = rod.length,
                clampToRangeOnCommit = true,
                formatValue = { formatTableValue(it) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier.width(88.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            MetricChip(
                label = stringResource(Res.string.postprocessor_col_axial_force),
                value = formatTableValue(section.axialForce),
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label = stringResource(Res.string.postprocessor_col_normal_stress),
                value = formatTableValue(section.normalStress),
                isError = rodStrength != null && abs(section.normalStress) > rodStrength.allowableStress + 1e-9,
                modifier = Modifier.weight(1f),
            )
            MetricChip(
                label = stringResource(Res.string.postprocessor_col_displacement),
                value = formatTableValue(section.displacement),
                modifier = Modifier.weight(1f),
            )
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
                label = stringResource(Res.string.postprocessor_peak_axial_force),
                value = formatTableValue(summary.maxAbsAxialForce),
                modifier = Modifier.weight(1f),
            )
            PeakMetricCard(
                label = stringResource(Res.string.postprocessor_peak_normal_stress),
                value = formatTableValue(summary.maxAbsNormalStress),
                modifier = Modifier.weight(1f),
            )
            PeakMetricCard(
                label = stringResource(Res.string.postprocessor_peak_displacement),
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
                        text = stringResource(
                            Res.string.postprocessor_safety_factor_badge,
                            formatTableValue(minSafetyFactor),
                        ),
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
