package ru.sotber.sapr_wolf.ui.components.mainAppBar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.ui.CalculationState
import ru.sotber.sapr_wolf.ui.formatCalculationValue
import ru.sotber.sapr_wolf.ui.theme.CapsuleShape
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeSpacing
import ru.sotber.sapr_wolf.ui.theme.MonoSmallTextStyle
import ru.sotber.sapr_wolf.ui.theme.Shapes
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.processor_nodes_table_title
import sapr_wolf.shared.generated.resources.processor_support_badge
import sapr_wolf.shared.generated.resources.processor_table_col_displacement
import sapr_wolf.shared.generated.resources.processor_table_col_force
import sapr_wolf.shared.generated.resources.processor_table_col_node
import sapr_wolf.shared.generated.resources.processor_table_col_reaction
import kotlin.math.abs

private object ProcessorNodesTableDefaults {
    val maxListHeight = 200.dp
    const val nodeColWeight = 1.15f
    const val valueColWeight = 1f
}

@Composable
fun MainAppBarProcessorNodesTable(
    hazeState: HazeState?,
    construction: Construction,
    result: CalculationState.Success,
    selectedNodeIndex: Int?,
    onSelectNode: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalNodes = construction.nodeCount

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
                text = stringResource(Res.string.processor_nodes_table_title),
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
                    text = totalNodes.toString(),
                    style = MonoSmallTextStyle.copy(color = MaterialTheme.colorScheme.primary),
                    maxLines = 1,
                )
            }
        }

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline,
        )

        ProcessorNodesTableHeader()

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = ProcessorNodesTableDefaults.maxListHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            for (nodeIndex in 0 until totalNodes) {
                val displacement = result.displacements.getOrNull(nodeIndex) ?: 0.0
                val force = construction.nodeForces.getOrElse(nodeIndex) { 0.0 }
                val reaction = result.reactions[nodeIndex]
                val isFixed = nodeIndex in construction.fixedNodes

                ProcessorNodeResultRow(
                    nodeIndex = nodeIndex,
                    displacement = displacement,
                    force = force,
                    reaction = reaction,
                    isFixed = isFixed,
                    isSelected = selectedNodeIndex == nodeIndex,
                    onClick = { onSelectNode(nodeIndex) },
                )
            }
        }
    }
}

@Composable
private fun ProcessorNodesTableHeader(
    modifier: Modifier = Modifier,
) {
    val headerStyle = MonoSmallTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialThemeSpacing.small),
    ) {
        Text(
            text = stringResource(Res.string.processor_table_col_node),
            style = headerStyle,
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.nodeColWeight),
        )

        Text(
            text = stringResource(Res.string.processor_table_col_displacement),
            style = headerStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.valueColWeight),
        )

        Text(
            text = stringResource(Res.string.processor_table_col_force),
            style = headerStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.valueColWeight),
        )

        Text(
            text = stringResource(Res.string.processor_table_col_reaction),
            style = headerStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.valueColWeight),
        )
    }
}

@Composable
private fun ProcessorNodeResultRow(
    nodeIndex: Int,
    displacement: Double,
    force: Double,
    reaction: Double?,
    isFixed: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colorScheme = MaterialTheme.colorScheme
    val extraColors = MaterialThemeExtraColors

    val rowBackgroundColor = when {
        isSelected -> colorScheme.primaryContainer
        isHovered -> colorScheme.surfaceVariant.copy(alpha = 0.65f)
        else -> Color.Transparent
    }

    val primaryTextColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
    val mutedTextColor = if (isSelected) {
        colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
    } else {
        colorScheme.onSurfaceVariant
    }

    val displacementColor = when {
        isSelected -> colorScheme.onPrimaryContainer
        abs(displacement) > 1e-9 -> colorScheme.primary
        else -> mutedTextColor
    }

    val forceColor = if (abs(force) > 1e-9) primaryTextColor else mutedTextColor
    val reactionText = reaction?.let(::formatCalculationValue) ?: "—"
    val reactionColor = if (reaction != null) primaryTextColor else mutedTextColor

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.small)
            .background(rowBackgroundColor)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = MaterialThemeSpacing.small,
                vertical = MaterialThemeSpacing.extraSmall,
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialThemeSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.nodeColWeight),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        color = if (isSelected) colorScheme.primary else extraColors.glassChip,
                        shape = Shapes.extraSmall,
                    )
                    .padding(
                        horizontal = MaterialThemeSpacing.small,
                        vertical = 2.dp,
                    ),
            ) {
                Text(
                    text = "${nodeIndex + 1}",
                    style = MonoSmallTextStyle.copy(
                        color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                    ),
                    maxLines = 1,
                )
            }

            if (isFixed) {
                Text(
                    text = stringResource(Res.string.processor_support_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = formatCalculationValue(displacement),
            style = MonoSmallTextStyle.copy(color = displacementColor),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.valueColWeight),
        )

        Text(
            text = formatCalculationValue(force),
            style = MonoSmallTextStyle.copy(color = forceColor),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.valueColWeight),
        )

        Text(
            text = reactionText,
            style = MonoSmallTextStyle.copy(color = reactionColor),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(ProcessorNodesTableDefaults.valueColWeight),
        )
    }
}
