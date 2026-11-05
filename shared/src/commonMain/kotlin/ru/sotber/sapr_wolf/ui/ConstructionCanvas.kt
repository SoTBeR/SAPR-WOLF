package ru.sotber.sapr_wolf.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.model.nodePositionOnAxis
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MonoSmallTextStyle
import kotlin.math.abs

private object CanvasDefaults {
    const val DOT_GRID_STEP = 32f
    const val DOT_GRID_RADIUS = 1.2f
    const val ZOOM_STEP_FACTOR = 1.15f

    const val BASE_FONT_SIZE_SP = 12f
    const val MIN_STROKE_WIDTH = 0.75f
    const val FLOAT_EPSILON = 1e-9

    const val BASE_ROD_HEIGHT_PER_AREA = 32.0
    const val MIN_BASE_ROD_HEIGHT = 20f
    const val MAX_BASE_ROD_HEIGHT = 180f
    const val BASE_ROD_CORNER_RADIUS = 4f
    const val BASE_ROD_STROKE_WIDTH = 1.4f
    const val BASE_ROD_SELECTED_STROKE_WIDTH = 2.2f
    const val BASE_ROD_BADGE_RADIUS = 11f
    const val BASE_ROD_BADGE_STROKE_WIDTH = 1f
    const val MIN_ROD_HIT_HEIGHT = 14f

    const val BASE_DIMENSION_OFFSET_Y = 24f
    const val BASE_DIMENSION_NOTCH_HALF_LEN = 4f
    const val BASE_DIMENSION_TEXT_GAP_Y = 4f
    const val BASE_DASH_ON = 4f
    const val BASE_DASH_OFF = 3f
    const val BASE_LINE_STROKE_WIDTH = 1f

    const val BASE_NODE_SIZE = 20f
    const val BASE_NODE_CORNER_RADIUS = 3f
    const val BASE_NODE_STROKE_WIDTH = 1.2f
    const val BASE_NODE_SELECTED_STROKE_WIDTH = 2f
    const val MIN_NODE_HIT_HALF_SIZE = 10f

    const val BASE_SUPPORT_MIN_HEIGHT = 84f
    const val BASE_SUPPORT_EXTRA_MARGIN = 20f
    const val BASE_SUPPORT_WALL_STROKE = 2.4f
    const val BASE_SUPPORT_HATCH_LEN = 9f
    const val BASE_SUPPORT_HATCH_STEP = 7f
    const val BASE_SUPPORT_HATCH_INSET = 4f
    const val BASE_SUPPORT_HATCH_STROKE = 1.4f

    const val BASE_NODAL_FORCE_ARROW_LEN = 52f
    const val BASE_NODAL_FORCE_SHAFT_STROKE = 2.4f
    const val BASE_NODAL_FORCE_TIP_LEN = 9f
    const val BASE_NODAL_FORCE_TIP_HALF_HEIGHT = 5f
    const val BASE_NODAL_FORCE_TEXT_GAP_Y = 4f

    const val BASE_Q_TWO_ROWS_HEIGHT = 44f
    const val BASE_Q_THREE_ROWS_HEIGHT = 84f
    const val MIN_SCREEN_HEIGHT_TWO_ROWS = 18f
    const val MIN_SCREEN_HEIGHT_THREE_ROWS = 34f
    const val BASE_Q_PAD_X = 12f
    const val BASE_Q_ARROW_LEN = 16f
    const val BASE_Q_ARROW_GAP = 10f
    const val BASE_Q_TIP_LEN = 5f
    const val BASE_Q_TIP_WING = 3f
    const val BASE_Q_SHAFT_STROKE = 1.5f
    const val BASE_Q_TEXT_GAP_Y = 4f
    const val MAX_Q_ARROWS_PER_ROW = 24

    const val BASE_ADD_BUTTON_OFFSET = 44f
    const val BASE_ADD_BUTTON_RADIUS = 18f
    const val BASE_ADD_BUTTON_PLUS_HALF_LEN = 7f
    const val BASE_ADD_BUTTON_BORDER_STROKE = 1.5f
    const val BASE_ADD_BUTTON_PLUS_STROKE = 2f
    const val MIN_ADD_BUTTON_HIT_RADIUS = 12f
}

private data class CanvasPalette(
    val canvasBase: Color,
    val dots: Color,
    val axis: Color,
    val rodFill: Color,
    val rodSelectedFill: Color,
    val rodBorder: Color,
    val rodSelectedBorder: Color,
    val nodeFill: Color,
    val nodeSelectedFill: Color,
    val nodeBorder: Color,
    val nodeSelectedBorder: Color,
    val onNode: Color,
    val onSelectedNode: Color,
    val forceAndLoad: Color,
    val dimension: Color,
    val supportHatch: Color,
    val addButtonFill: Color,
    val addButtonBorder: Color,
    val addButtonIcon: Color,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConstructionCanvas(
    construction: Construction,
    viewportState: ViewportState,
    selectedRodIndex: Int?,
    selectedNodeIndex: Int?,
    onSelectRod: (Int) -> Unit,
    onSelectNode: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onAddRodLeft: () -> Unit,
    onAddRodRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MaterialTheme.colorScheme
    val extraColors = MaterialThemeExtraColors

    val palette = remember(colorScheme, extraColors) {
        CanvasPalette(
            canvasBase = extraColors.canvasBase,
            dots = extraColors.canvasDots,
            axis = colorScheme.outlineVariant,
            rodFill = colorScheme.surfaceVariant,
            rodSelectedFill = colorScheme.primaryContainer,
            rodBorder = colorScheme.outlineVariant,
            rodSelectedBorder = colorScheme.primary,
            nodeFill = extraColors.glassChip,
            nodeSelectedFill = colorScheme.primary,
            nodeBorder = colorScheme.outlineVariant,
            nodeSelectedBorder = colorScheme.primary,
            onNode = colorScheme.onSurface,
            onSelectedNode = colorScheme.onPrimary,
            forceAndLoad = colorScheme.primary,
            dimension = colorScheme.onSurfaceVariant,
            supportHatch = extraColors.supportHatch,
            addButtonFill = colorScheme.surfaceVariant,
            addButtonBorder = colorScheme.outlineVariant,
            addButtonIcon = colorScheme.primary,
        )
    }

    val totalLength = remember(construction.rods) {
        construction.rods.sumOf { it.length }
    }

    val currentConstruction by rememberUpdatedState(construction)
    val currentTotalLength by rememberUpdatedState(totalLength)
    val currentOnSelectRod by rememberUpdatedState(onSelectRod)
    val currentOnSelectNode by rememberUpdatedState(onSelectNode)
    val currentOnClearSelection by rememberUpdatedState(onClearSelection)
    val currentOnAddRodLeft by rememberUpdatedState(onAddRodLeft)
    val currentOnAddRodRight by rememberUpdatedState(onAddRodRight)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { intSize ->
                if (!viewportState.isInitialized && intSize.width > 0) {
                    viewportState.fitToView(intSize.width.toFloat(), currentTotalLength)
                }
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                val delta = if (change.scrollDelta.y != 0f) change.scrollDelta.y else change.scrollDelta.x
                if (delta == 0f) return@onPointerEvent

                val zoomFactorChange = if (delta < 0f) {
                    CanvasDefaults.ZOOM_STEP_FACTOR
                } else {
                    1f / CanvasDefaults.ZOOM_STEP_FACTOR
                }

                viewportState.zoomAt(
                    cursorScreenX = change.position.x,
                    cursorScreenY = change.position.y,
                    canvasWidth = size.width.toFloat(),
                    canvasHeight = size.height.toFloat(),
                    zoomFactorChange = zoomFactorChange,
                    horizontalOnly = event.keyboardModifiers.isShiftPressed,
                )
            }
            .pointerInput(viewportState) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    viewportState.pan(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(viewportState) {
                detectTapGestures { tapOffset ->
                    handleCanvasTap(
                        tapOffset = tapOffset,
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat(),
                        construction = currentConstruction,
                        totalLength = currentTotalLength,
                        viewportState = viewportState,
                        onSelectRod = currentOnSelectRod,
                        onSelectNode = currentOnSelectNode,
                        onClearSelection = currentOnClearSelection,
                        onAddRodLeft = currentOnAddRodLeft,
                        onAddRodRight = currentOnAddRodRight,
                    )
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        if (!viewportState.isInitialized && canvasWidth > 0f) {
            viewportState.fitToView(canvasWidth, totalLength)
        }

        val centerY = canvasHeight / 2f + viewportState.panY
        val scale = viewportState.scaleFactor
        val scaledMonoStyle = MonoSmallTextStyle.copy(
            fontSize = (CanvasDefaults.BASE_FONT_SIZE_SP * scale).sp
        )

        drawDotGrid(
            dotsColor = palette.dots,
            width = canvasWidth,
            height = canvasHeight,
            panX = viewportState.panX,
            panY = viewportState.panY,
        )

        if (construction.rods.isEmpty()) {
            drawAddButton(
                buttonCenterX = canvasWidth / 2f + viewportState.panX,
                centerY = centerY,
                scale = scale,
                palette = palette,
            )
            return@Canvas
        }

        val totalNodes = construction.nodeCount
        val buttonLeftX = leftAddButtonCenterX(construction, viewportState, canvasWidth, totalLength, scale)
        val buttonRightX = rightAddButtonCenterX(construction, viewportState, canvasWidth, totalLength, scale)

        val axisStartX = buttonLeftX.coerceAtLeast(0f)
        val axisEndX = buttonRightX.coerceAtMost(canvasWidth)
        if (axisEndX > axisStartX) {
            drawLine(
                color = palette.axis,
                start = Offset(axisStartX, centerY),
                end = Offset(axisEndX, centerY),
                strokeWidth = scaledStroke(CanvasDefaults.BASE_LINE_STROKE_WIDTH, scale),
            )
        }

        val maxRodHeight = construction.rods.maxOf { rod -> rodScreenHeight(rod, scale) }
        val commonDimY = centerY + maxRodHeight / 2f + CanvasDefaults.BASE_DIMENSION_OFFSET_Y * scale

        drawRodsAndDimensions(
            construction = construction,
            viewportState = viewportState,
            selectedRodIndex = selectedRodIndex,
            canvasWidth = canvasWidth,
            totalLength = totalLength,
            centerY = centerY,
            commonDimY = commonDimY,
            scale = scale,
            palette = palette,
            textMeasurer = textMeasurer,
            textStyle = scaledMonoStyle,
        )

        drawDimensionExtensionLines(
            construction = construction,
            viewportState = viewportState,
            canvasWidth = canvasWidth,
            totalLength = totalLength,
            centerY = centerY,
            commonDimY = commonDimY,
            scale = scale,
            palette = palette,
        )

        for (fixedNodeIdx in construction.fixedNodes) {
            if (fixedNodeIdx in 0 until totalNodes) {
                val nodeX = viewportState.worldToScreenX(
                    worldX = construction.nodePositionOnAxis(fixedNodeIdx),
                    canvasWidth = canvasWidth,
                    totalConstructionLength = totalLength,
                )
                val adjacentBaseHeight = maxAdjacentBaseRodHeight(construction, fixedNodeIdx)
                drawFixedSupport(
                    nodeX = nodeX,
                    centerY = centerY,
                    isLeftEdge = fixedNodeIdx == 0,
                    isRightEdge = fixedNodeIdx == totalNodes - 1,
                    adjacentBaseRodHeight = adjacentBaseHeight,
                    scale = scale,
                    hatchColor = palette.supportHatch,
                )
            }
        }

        for (nodeIdx in construction.nodeForces.indices) {
            if (nodeIdx !in 0 until totalNodes) continue
            val force = construction.nodeForces[nodeIdx]
            if (abs(force) > CanvasDefaults.FLOAT_EPSILON) {
                val nodeX = viewportState.worldToScreenX(
                    worldX = construction.nodePositionOnAxis(nodeIdx),
                    canvasWidth = canvasWidth,
                    totalConstructionLength = totalLength,
                )
                drawNodalForce(
                    textMeasurer = textMeasurer,
                    force = force,
                    nodeX = nodeX,
                    centerY = centerY,
                    scale = scale,
                    color = palette.forceAndLoad,
                    textStyle = scaledMonoStyle,
                )
            }
        }

        drawNodes(
            construction = construction,
            viewportState = viewportState,
            selectedNodeIndex = selectedNodeIndex,
            canvasWidth = canvasWidth,
            totalLength = totalLength,
            centerY = centerY,
            scale = scale,
            palette = palette,
            textMeasurer = textMeasurer,
            textStyle = scaledMonoStyle,
        )

        drawAddButton(
            buttonCenterX = buttonLeftX,
            centerY = centerY,
            scale = scale,
            palette = palette,
        )
        drawAddButton(
            buttonCenterX = buttonRightX,
            centerY = centerY,
            scale = scale,
            palette = palette,
        )
    }
}

private fun handleCanvasTap(
    tapOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    construction: Construction,
    totalLength: Double,
    viewportState: ViewportState,
    onSelectRod: (Int) -> Unit,
    onSelectNode: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onAddRodLeft: () -> Unit,
    onAddRodRight: () -> Unit,
) {
    val centerY = canvasHeight / 2f + viewportState.panY
    val scale = viewportState.scaleFactor
    val addButtonHitRadius = (CanvasDefaults.BASE_ADD_BUTTON_RADIUS * scale)
        .coerceAtLeast(CanvasDefaults.MIN_ADD_BUTTON_HIT_RADIUS)

    if (construction.rods.isEmpty()) {
        val centerButton = Offset(canvasWidth / 2f + viewportState.panX, centerY)
        if ((tapOffset - centerButton).getDistance() <= addButtonHitRadius) {
            onAddRodRight()
        } else {
            onClearSelection()
        }
        return
    }

    val totalNodes = construction.nodeCount
    val nodeHitHalfSize = (CanvasDefaults.BASE_NODE_SIZE * scale / 2f)
        .coerceAtLeast(CanvasDefaults.MIN_NODE_HIT_HALF_SIZE)

    for (nodeIdx in 0 until totalNodes) {
        val nodeX = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(nodeIdx),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        if (abs(tapOffset.x - nodeX) <= nodeHitHalfSize && abs(tapOffset.y - centerY) <= nodeHitHalfSize) {
            onSelectNode(nodeIdx)
            return
        }
    }

    val leftButtonCenter = Offset(
        x = leftAddButtonCenterX(construction, viewportState, canvasWidth, totalLength, scale),
        y = centerY,
    )
    if ((tapOffset - leftButtonCenter).getDistance() <= addButtonHitRadius) {
        onAddRodLeft()
        return
    }

    val rightButtonCenter = Offset(
        x = rightAddButtonCenterX(construction, viewportState, canvasWidth, totalLength, scale),
        y = centerY,
    )
    if ((tapOffset - rightButtonCenter).getDistance() <= addButtonHitRadius) {
        onAddRodRight()
        return
    }

    for (rodIdx in construction.rods.indices) {
        val rod = construction.rods[rodIdx]
        val xLeft = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(rodIdx),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        val xRight = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(rodIdx + 1),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        val hitHalfHeight = rodScreenHeight(rod, scale)
            .coerceAtLeast(CanvasDefaults.MIN_ROD_HIT_HEIGHT) / 2f

        if (tapOffset.x in xLeft..xRight && abs(tapOffset.y - centerY) <= hitHalfHeight) {
            onSelectRod(rodIdx)
            return
        }
    }

    onClearSelection()
}

private fun baseRodHeight(rod: Rod): Float {
    return (rod.crossSectionArea * CanvasDefaults.BASE_ROD_HEIGHT_PER_AREA)
        .toFloat()
        .coerceIn(CanvasDefaults.MIN_BASE_ROD_HEIGHT, CanvasDefaults.MAX_BASE_ROD_HEIGHT)
}

private fun rodScreenHeight(rod: Rod, scale: Float): Float = baseRodHeight(rod) * scale

private fun maxAdjacentBaseRodHeight(construction: Construction, nodeIndex: Int): Float {
    var result = 0f
    if (nodeIndex - 1 in construction.rods.indices) {
        result = maxOf(result, baseRodHeight(construction.rods[nodeIndex - 1]))
    }
    if (nodeIndex in construction.rods.indices) {
        result = maxOf(result, baseRodHeight(construction.rods[nodeIndex]))
    }
    return result
}

private fun leftAddButtonCenterX(
    construction: Construction,
    viewportState: ViewportState,
    canvasWidth: Float,
    totalLength: Double,
    scale: Float,
): Float {
    val firstNodeX = viewportState.worldToScreenX(0.0, canvasWidth, totalLength)
    val firstNodeForce = construction.nodeForces.firstOrNull() ?: 0.0
    val forceClearance = if (firstNodeForce > CanvasDefaults.FLOAT_EPSILON) {
        CanvasDefaults.BASE_NODAL_FORCE_ARROW_LEN * scale
    } else {
        0f
    }
    return firstNodeX - CanvasDefaults.BASE_ADD_BUTTON_OFFSET * scale - forceClearance
}

private fun rightAddButtonCenterX(
    construction: Construction,
    viewportState: ViewportState,
    canvasWidth: Float,
    totalLength: Double,
    scale: Float,
): Float {
    val lastNodeX = viewportState.worldToScreenX(totalLength, canvasWidth, totalLength)
    val lastNodeIdx = construction.nodeCount - 1
    val lastNodeForce = construction.nodeForces.getOrNull(lastNodeIdx) ?: 0.0
    val forceClearance = if (lastNodeForce < -CanvasDefaults.FLOAT_EPSILON) {
        CanvasDefaults.BASE_NODAL_FORCE_ARROW_LEN * scale
    } else {
        0f
    }
    return lastNodeX + CanvasDefaults.BASE_ADD_BUTTON_OFFSET * scale + forceClearance
}

private fun scaledStroke(baseWidth: Float, scale: Float): Float {
    return (baseWidth * scale).coerceAtLeast(CanvasDefaults.MIN_STROKE_WIDTH)
}

private fun DrawScope.drawDotGrid(
    dotsColor: Color,
    width: Float,
    height: Float,
    panX: Float,
    panY: Float,
) {
    val step = CanvasDefaults.DOT_GRID_STEP
    val startX = ((panX % step) + step) % step
    val startY = ((panY % step) + step) % step

    var currentX = startX
    while (currentX <= width) {
        var currentY = startY
        while (currentY <= height) {
            drawCircle(
                color = dotsColor,
                radius = CanvasDefaults.DOT_GRID_RADIUS,
                center = Offset(currentX, currentY),
            )
            currentY += step
        }
        currentX += step
    }
}

private fun DrawScope.drawRodsAndDimensions(
    construction: Construction,
    viewportState: ViewportState,
    selectedRodIndex: Int?,
    canvasWidth: Float,
    totalLength: Double,
    centerY: Float,
    commonDimY: Float,
    scale: Float,
    palette: CanvasPalette,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
) {
    val cornerRadius = CornerRadius(CanvasDefaults.BASE_ROD_CORNER_RADIUS * scale)
    val notchHalfLen = CanvasDefaults.BASE_DIMENSION_NOTCH_HALF_LEN * scale
    val dimStroke = scaledStroke(CanvasDefaults.BASE_LINE_STROKE_WIDTH, scale)
    val badgeRadius = CanvasDefaults.BASE_ROD_BADGE_RADIUS * scale

    for (rodIdx in construction.rods.indices) {
        val rod = construction.rods[rodIdx]
        val xLeft = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(rodIdx),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        val xRight = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(rodIdx + 1),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        val rodWidth = (xRight - xLeft).coerceAtLeast(1f)
        val baseHeight = baseRodHeight(rod)
        val rodHeight = baseHeight * scale
        val rectTop = centerY - rodHeight / 2f
        val isSelected = selectedRodIndex == rodIdx

        drawRoundRect(
            color = palette.canvasBase,
            topLeft = Offset(xLeft, rectTop),
            size = Size(rodWidth, rodHeight),
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = if (isSelected) palette.rodSelectedFill else palette.rodFill,
            topLeft = Offset(xLeft, rectTop),
            size = Size(rodWidth, rodHeight),
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = if (isSelected) palette.rodSelectedBorder else palette.rodBorder,
            topLeft = Offset(xLeft, rectTop),
            size = Size(rodWidth, rodHeight),
            cornerRadius = cornerRadius,
            style = Stroke(
                width = scaledStroke(
                    baseWidth = if (isSelected) {
                        CanvasDefaults.BASE_ROD_SELECTED_STROKE_WIDTH
                    } else {
                        CanvasDefaults.BASE_ROD_STROKE_WIDTH
                    },
                    scale = scale,
                )
            ),
        )

        if (abs(rod.distributedLoad) > CanvasDefaults.FLOAT_EPSILON) {
            drawDistributedLoad(
                textMeasurer = textMeasurer,
                load = rod.distributedLoad,
                xLeft = xLeft,
                xRight = xRight,
                centerY = centerY,
                rectTop = rectTop,
                baseRodHeight = baseHeight,
                rodHeight = rodHeight,
                scale = scale,
                color = palette.forceAndLoad,
                textStyle = textStyle,
            )
        }

        val centerX = (xLeft + xRight) / 2f
        if (rodWidth >= badgeRadius * 2.2f) {
            val badgeCenter = Offset(centerX, centerY)
            drawCircle(
                color = palette.canvasBase,
                radius = badgeRadius,
                center = badgeCenter,
            )
            drawCircle(
                color = palette.rodFill,
                radius = badgeRadius,
                center = badgeCenter,
            )
            drawCircle(
                color = palette.rodBorder,
                radius = badgeRadius,
                center = badgeCenter,
                style = Stroke(width = scaledStroke(CanvasDefaults.BASE_ROD_BADGE_STROKE_WIDTH, scale)),
            )

            val rodNumberText = textMeasurer.measure(
                text = "${rodIdx + 1}",
                style = textStyle.copy(color = palette.onNode),
            )
            drawText(
                textLayoutResult = rodNumberText,
                topLeft = Offset(
                    x = centerX - rodNumberText.size.width / 2f,
                    y = centerY - rodNumberText.size.height / 2f,
                ),
            )
        }

        drawLine(
            color = palette.dimension,
            start = Offset(xLeft, commonDimY),
            end = Offset(xRight, commonDimY),
            strokeWidth = dimStroke,
        )
        drawLine(
            color = palette.dimension,
            start = Offset(xLeft, commonDimY - notchHalfLen),
            end = Offset(xLeft, commonDimY + notchHalfLen),
            strokeWidth = dimStroke,
        )
        drawLine(
            color = palette.dimension,
            start = Offset(xRight, commonDimY - notchHalfLen),
            end = Offset(xRight, commonDimY + notchHalfLen),
            strokeWidth = dimStroke,
        )

        val lengthText = textMeasurer.measure(
            text = "L = ${rod.length}",
            style = textStyle.copy(color = palette.dimension),
        )
        drawText(
            textLayoutResult = lengthText,
            topLeft = Offset(
                x = centerX - lengthText.size.width / 2f,
                y = commonDimY + CanvasDefaults.BASE_DIMENSION_TEXT_GAP_Y * scale,
            ),
        )
    }
}

private fun DrawScope.drawDimensionExtensionLines(
    construction: Construction,
    viewportState: ViewportState,
    canvasWidth: Float,
    totalLength: Double,
    centerY: Float,
    commonDimY: Float,
    scale: Float,
    palette: CanvasPalette,
) {
    val dashEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(
            (CanvasDefaults.BASE_DASH_ON * scale).coerceAtLeast(1.5f),
            (CanvasDefaults.BASE_DASH_OFF * scale).coerceAtLeast(1.5f),
        ),
        phase = 0f,
    )
    val strokeWidth = scaledStroke(CanvasDefaults.BASE_LINE_STROKE_WIDTH, scale)
    val totalNodes = construction.nodeCount

    for (nodeIdx in 0 until totalNodes) {
        val nodeX = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(nodeIdx),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        val adjacentHeight = maxAdjacentBaseRodHeight(construction, nodeIdx) * scale
        val rodBottomAtNode = centerY + adjacentHeight / 2f

        if (commonDimY > rodBottomAtNode) {
            drawLine(
                color = palette.dimension,
                start = Offset(nodeX, rodBottomAtNode),
                end = Offset(nodeX, commonDimY),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect,
            )
        }
    }
}

private fun DrawScope.drawFixedSupport(
    nodeX: Float,
    centerY: Float,
    isLeftEdge: Boolean,
    isRightEdge: Boolean,
    adjacentBaseRodHeight: Float,
    scale: Float,
    hatchColor: Color,
) {
    val baseSupportHeight = maxOf(
        CanvasDefaults.BASE_SUPPORT_MIN_HEIGHT,
        adjacentBaseRodHeight + CanvasDefaults.BASE_SUPPORT_EXTRA_MARGIN,
    )
    val barHeight = baseSupportHeight * scale
    val hatchLen = CanvasDefaults.BASE_SUPPORT_HATCH_LEN * scale
    val hatchStep = (CanvasDefaults.BASE_SUPPORT_HATCH_STEP * scale).coerceAtLeast(2f)
    val hatchInset = CanvasDefaults.BASE_SUPPORT_HATCH_INSET * scale

    drawLine(
        color = hatchColor,
        start = Offset(nodeX, centerY - barHeight / 2f),
        end = Offset(nodeX, centerY + barHeight / 2f),
        strokeWidth = scaledStroke(CanvasDefaults.BASE_SUPPORT_WALL_STROKE, scale),
    )

    val directions = when {
        isLeftEdge -> listOf(-1f)
        isRightEdge -> listOf(1f)
        else -> listOf(-1f, 1f)
    }

    val hatchStroke = scaledStroke(CanvasDefaults.BASE_SUPPORT_HATCH_STROKE, scale)
    for (dir in directions) {
        var currentY = centerY - barHeight / 2f + hatchInset
        val endY = centerY + barHeight / 2f - hatchInset
        while (currentY <= endY) {
            drawLine(
                color = hatchColor,
                start = Offset(nodeX, currentY),
                end = Offset(nodeX + dir * hatchLen, currentY + dir * hatchLen),
                strokeWidth = hatchStroke,
            )
            currentY += hatchStep
        }
    }
}

private fun DrawScope.drawNodalForce(
    textMeasurer: TextMeasurer,
    force: Double,
    nodeX: Float,
    centerY: Float,
    scale: Float,
    color: Color,
    textStyle: TextStyle,
) {
    val direction = if (force > 0) 1f else -1f
    val nodeHalfSize = CanvasDefaults.BASE_NODE_SIZE * scale / 2f
    val arrowLen = CanvasDefaults.BASE_NODAL_FORCE_ARROW_LEN * scale
    val endX = nodeX - direction * nodeHalfSize
    val startX = endX - direction * arrowLen
    val tipLen = CanvasDefaults.BASE_NODAL_FORCE_TIP_LEN * scale
    val tipHalfHeight = CanvasDefaults.BASE_NODAL_FORCE_TIP_HALF_HEIGHT * scale

    drawLine(
        color = color,
        start = Offset(startX, centerY),
        end = Offset(endX - direction * (tipLen * 0.5f), centerY),
        strokeWidth = scaledStroke(CanvasDefaults.BASE_NODAL_FORCE_SHAFT_STROKE, scale),
    )

    val arrowPath = Path().apply {
        moveTo(endX, centerY)
        lineTo(endX - direction * tipLen, centerY - tipHalfHeight)
        lineTo(endX - direction * tipLen, centerY + tipHalfHeight)
        close()
    }
    drawPath(path = arrowPath, color = color)

    val text = textMeasurer.measure("F = $force", textStyle.copy(color = color))
    drawText(
        textLayoutResult = text,
        topLeft = Offset(
            x = startX - if (direction < 0f) 0f else text.size.width.toFloat(),
            y = centerY - text.size.height - CanvasDefaults.BASE_NODAL_FORCE_TEXT_GAP_Y * scale,
        ),
    )
}

private fun DrawScope.drawDistributedLoad(
    textMeasurer: TextMeasurer,
    load: Double,
    xLeft: Float,
    xRight: Float,
    centerY: Float,
    rectTop: Float,
    baseRodHeight: Float,
    rodHeight: Float,
    scale: Float,
    color: Color,
    textStyle: TextStyle,
) {
    val direction = if (load > 0) 1f else -1f
    val rodWidth = (xRight - xLeft).coerceAtLeast(1f)

    val desiredRows = when {
        baseRodHeight < CanvasDefaults.BASE_Q_TWO_ROWS_HEIGHT -> 1
        baseRodHeight < CanvasDefaults.BASE_Q_THREE_ROWS_HEIGHT -> 2
        else -> 3
    }
    val maxRowsByScreenSpace = when {
        rodHeight < CanvasDefaults.MIN_SCREEN_HEIGHT_TWO_ROWS -> 1
        rodHeight < CanvasDefaults.MIN_SCREEN_HEIGHT_THREE_ROWS -> 2
        else -> 3
    }
    val rowCount = minOf(desiredRows, maxRowsByScreenSpace)
    val yRows = when (rowCount) {
        1 -> listOf(centerY)
        2 -> listOf(centerY - rodHeight * 0.24f, centerY + rodHeight * 0.24f)
        else -> listOf(centerY - rodHeight * 0.28f, centerY, centerY + rodHeight * 0.28f)
    }

    val padX = (CanvasDefaults.BASE_Q_PAD_X * scale).coerceAtMost(rodWidth * 0.2f)
    val availableWidth = rodWidth - 2f * padX
    if (availableWidth >= 3f) {
        val targetArrowLen = CanvasDefaults.BASE_Q_ARROW_LEN * scale
        val targetGap = CanvasDefaults.BASE_Q_ARROW_GAP * scale
        val count = ((availableWidth + targetGap) / (targetArrowLen + targetGap))
            .toInt()
            .coerceIn(1, CanvasDefaults.MAX_Q_ARROWS_PER_ROW)
        val step = availableWidth / count
        val arrowLen = targetArrowLen.coerceAtMost(step * 0.8f)
        val tipLen = (CanvasDefaults.BASE_Q_TIP_LEN * scale).coerceAtMost(arrowLen * 0.5f)
        val tipWing = (CanvasDefaults.BASE_Q_TIP_WING * scale).coerceAtMost(rodHeight * 0.35f)
        val shaftStroke = scaledStroke(CanvasDefaults.BASE_Q_SHAFT_STROKE, scale)

        for (y in yRows) {
            for (i in 0 until count) {
                val intervalCenter = (xLeft + padX) + (i + 0.5f) * step
                val arrowStartX = intervalCenter - direction * (arrowLen / 2f)
                val arrowEndX = intervalCenter + direction * (arrowLen / 2f)

                drawLine(
                    color = color,
                    start = Offset(arrowStartX, y),
                    end = Offset(arrowEndX - direction * (tipLen * 0.4f), y),
                    strokeWidth = shaftStroke,
                )

                val arrowPath = Path().apply {
                    moveTo(arrowEndX, y)
                    lineTo(arrowEndX - direction * tipLen, y - tipWing)
                    lineTo(arrowEndX - direction * tipLen, y + tipWing)
                    close()
                }
                drawPath(path = arrowPath, color = color)
            }
        }
    }

    val text = textMeasurer.measure("q = $load", textStyle.copy(color = color))
    drawText(
        textLayoutResult = text,
        topLeft = Offset(
            x = (xLeft + xRight) / 2f - text.size.width / 2f,
            y = rectTop - text.size.height - CanvasDefaults.BASE_Q_TEXT_GAP_Y * scale,
        ),
    )
}

private fun DrawScope.drawNodes(
    construction: Construction,
    viewportState: ViewportState,
    selectedNodeIndex: Int?,
    canvasWidth: Float,
    totalLength: Double,
    centerY: Float,
    scale: Float,
    palette: CanvasPalette,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
) {
    val squareSize = CanvasDefaults.BASE_NODE_SIZE * scale
    val cornerRadius = CornerRadius(CanvasDefaults.BASE_NODE_CORNER_RADIUS * scale)
    val totalNodes = construction.nodeCount

    for (nodeIdx in 0 until totalNodes) {
        val nodeX = viewportState.worldToScreenX(
            worldX = construction.nodePositionOnAxis(nodeIdx),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        val isSelected = selectedNodeIndex == nodeIdx
        val topLeft = Offset(nodeX - squareSize / 2f, centerY - squareSize / 2f)
        val nodeSize = Size(squareSize, squareSize)

        drawRoundRect(
            color = palette.canvasBase,
            topLeft = topLeft,
            size = nodeSize,
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = if (isSelected) palette.nodeSelectedFill else palette.nodeFill,
            topLeft = topLeft,
            size = nodeSize,
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = if (isSelected) palette.nodeSelectedBorder else palette.nodeBorder,
            topLeft = topLeft,
            size = nodeSize,
            cornerRadius = cornerRadius,
            style = Stroke(
                width = scaledStroke(
                    baseWidth = if (isSelected) {
                        CanvasDefaults.BASE_NODE_SELECTED_STROKE_WIDTH
                    } else {
                        CanvasDefaults.BASE_NODE_STROKE_WIDTH
                    },
                    scale = scale,
                )
            ),
        )

        val nodeNumberText = textMeasurer.measure(
            text = "${nodeIdx + 1}",
            style = textStyle.copy(
                color = if (isSelected) palette.onSelectedNode else palette.onNode
            ),
        )
        drawText(
            textLayoutResult = nodeNumberText,
            topLeft = Offset(
                x = nodeX - nodeNumberText.size.width / 2f,
                y = centerY - nodeNumberText.size.height / 2f,
            ),
        )
    }
}

private fun DrawScope.drawAddButton(
    buttonCenterX: Float,
    centerY: Float,
    scale: Float,
    palette: CanvasPalette,
) {
    val radius = CanvasDefaults.BASE_ADD_BUTTON_RADIUS * scale
    val center = Offset(buttonCenterX, centerY)

    drawCircle(
        color = palette.canvasBase,
        radius = radius,
        center = center,
    )
    drawCircle(
        color = palette.addButtonFill,
        radius = radius,
        center = center,
    )
    drawCircle(
        color = palette.addButtonBorder,
        radius = radius,
        center = center,
        style = Stroke(width = scaledStroke(CanvasDefaults.BASE_ADD_BUTTON_BORDER_STROKE, scale)),
    )

    val plusHalfLen = CanvasDefaults.BASE_ADD_BUTTON_PLUS_HALF_LEN * scale
    val plusStroke = scaledStroke(CanvasDefaults.BASE_ADD_BUTTON_PLUS_STROKE, scale)
    drawLine(
        color = palette.addButtonIcon,
        start = Offset(buttonCenterX - plusHalfLen, centerY),
        end = Offset(buttonCenterX + plusHalfLen, centerY),
        strokeWidth = plusStroke,
    )
    drawLine(
        color = palette.addButtonIcon,
        start = Offset(buttonCenterX, centerY - plusHalfLen),
        end = Offset(buttonCenterX, centerY + plusHalfLen),
        strokeWidth = plusStroke,
    )
}
