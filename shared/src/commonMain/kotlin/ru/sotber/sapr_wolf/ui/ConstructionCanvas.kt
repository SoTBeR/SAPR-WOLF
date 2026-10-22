package ru.sotber.sapr_wolf.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.model.nodePositionOnAxis
import ru.sotber.sapr_wolf.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConstructionCanvas(
    construction: Construction,
    viewportState: ViewportState,
    selectedRodIndex: Int?,
    selectedNodeIndex: Int?,
    onSelectRod: (Int) -> Unit,
    onSelectNode: (Int) -> Unit,
    onAddRodLeft: () -> Unit,
    onAddRodRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val extraColors = MaterialThemeExtraColors
    val totalLength = construction.rods.sumOf { it.length }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val change = event.changes.firstOrNull() ?: return@onPointerEvent
                val delta = if (change.scrollDelta.y != 0f) change.scrollDelta.y else change.scrollDelta.x
                if (delta == 0f) return@onPointerEvent

                val isShift = event.keyboardModifiers.isShiftPressed
                val cursorX = change.position.x
                val cursorY = change.position.y
                val zoomChangeFactor = if (delta < 0f) 1.15f else 0.87f

                viewportState.zoomAt(
                    cursorScreenX = cursorX,
                    cursorScreenY = cursorY,
                    canvasWidth = size.width.toFloat(),
                    canvasHeight = size.height.toFloat(),
                    zoomFactorChange = zoomChangeFactor,
                    horizontalOnly = isShift
                )
            }
            .pointerInput(viewportState) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    viewportState.pan(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(construction, viewportState) {
                detectTapGestures { tapOffset ->
                    val canvasWidth = size.width.toFloat()
                    val centerY = size.height / 2f + viewportState.panY
                    val totalNodes = construction.nodeCount
                    val scale = viewportState.scaleFactor

                    val nodeClickRadius = (16f * scale).coerceIn(14f, 32f)
                    for (nodeIdx in 0 until totalNodes) {
                        val nodeX = viewportState.worldToScreenX(
                            worldX = construction.nodePositionOnAxis(nodeIdx),
                            canvasWidth = canvasWidth,
                            totalConstructionLength = totalLength
                        )
                        if (abs(tapOffset.x - nodeX) <= nodeClickRadius && abs(tapOffset.y - centerY) <= nodeClickRadius) {
                            onSelectNode(nodeIdx)
                            return@detectTapGestures
                        }
                    }

                    for (rodIdx in construction.rods.indices) {
                        val rod = construction.rods[rodIdx]
                        val xLeft = viewportState.worldToScreenX(
                            worldX = construction.nodePositionOnAxis(rodIdx),
                            canvasWidth = canvasWidth,
                            totalConstructionLength = totalLength
                        )
                        val xRight = viewportState.worldToScreenX(
                            worldX = construction.nodePositionOnAxis(rodIdx + 1),
                            canvasWidth = canvasWidth,
                            totalConstructionLength = totalLength
                        )
                        val height = ((rod.crossSectionArea * 32.0).toFloat() * scale).coerceIn(16f, 220f)

                        if (tapOffset.x in xLeft..xRight && tapOffset.y in (centerY - height / 2)..(centerY + height / 2)) {
                            onSelectRod(rodIdx)
                            return@detectTapGestures
                        }
                    }

                    if (construction.rods.isNotEmpty()) {
                        val firstNodeX = viewportState.worldToScreenX(
                            worldX = 0.0,
                            canvasWidth = canvasWidth,
                            totalConstructionLength = totalLength
                        )
                        val buttonLeftX = firstNodeX - (36f * scale).coerceIn(24f, 60f)
                        if (abs(tapOffset.x - buttonLeftX) <= 24f && abs(tapOffset.y - centerY) <= 24f) {
                            onAddRodLeft()
                            return@detectTapGestures
                        }

                        val lastNodeX = viewportState.worldToScreenX(
                            worldX = construction.nodePositionOnAxis(construction.rods.size),
                            canvasWidth = canvasWidth,
                            totalConstructionLength = totalLength
                        )
                        val buttonRightX = lastNodeX + (36f * scale).coerceIn(24f, 60f)
                        if (abs(tapOffset.x - buttonRightX) <= 24f && abs(tapOffset.y - centerY) <= 24f) {
                            onAddRodRight()
                            return@detectTapGestures
                        }
                    } else {
                        val centerButtonX = canvasWidth / 2f + viewportState.panX
                        if (abs(tapOffset.x - centerButtonX) <= 24f && abs(tapOffset.y - centerY) <= 24f) {
                            onAddRodRight()
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f + viewportState.panY
        val scale = viewportState.scaleFactor

        val scaledFontSize = (12f * scale).coerceIn(8f, 24f).sp
        val scaledMonoStyle = MonoSmallTextStyle.copy(fontSize = scaledFontSize)

        if (!viewportState.isInitialized && canvasWidth > 0f) {
            viewportState.fitToView(canvasWidth, totalLength)
        }

        drawDotGrid(extraColors.canvasDots, canvasWidth, canvasHeight, viewportState.panX, viewportState.panY)

        val totalNodes = construction.nodeCount
        if (totalNodes == 0) return@Canvas

        val axisStartX = viewportState.worldToScreenX(-0.5, canvasWidth, totalLength).coerceAtLeast(0f)
        val axisEndX = viewportState.worldToScreenX(totalLength + 0.8, canvasWidth, totalLength).coerceAtMost(canvasWidth)
        drawLine(
            color = OutlineVariantDark,
            start = Offset(axisStartX, centerY),
            end = Offset(axisEndX, centerY),
            strokeWidth = 1f
        )

        val maxRodHeight = construction.rods.maxOfOrNull { rod ->
            ((rod.crossSectionArea * 32.0).toFloat() * scale).coerceIn(16f, 220f)
        } ?: (32f * scale).coerceIn(16f, 220f)
        val maxBottomY = centerY + maxRodHeight / 2f
        val commonDimY = maxBottomY + (24f * scale).coerceIn(18f, 50f)
        val notchLen = (4f * scale).coerceIn(3f, 8f)
        val dashEffect = PathEffect.dashPathEffect(
            floatArrayOf((4f * scale).coerceIn(3f, 8f), (3f * scale).coerceIn(2f, 6f)),
            0f
        )

        for (rodIdx in construction.rods.indices) {
            val rod = construction.rods[rodIdx]
            val xLeft = viewportState.worldToScreenX(construction.nodePositionOnAxis(rodIdx), canvasWidth, totalLength)
            val xRight = viewportState.worldToScreenX(construction.nodePositionOnAxis(rodIdx + 1), canvasWidth, totalLength)
            val rodWidth = (xRight - xLeft).coerceAtLeast(2f)
            val rodHeight = ((rod.crossSectionArea * 32.0).toFloat() * scale).coerceIn(16f, 220f)

            val isSelected = selectedRodIndex == rodIdx

            val rectTop = centerY - rodHeight / 2f
            drawRoundRect(
                color = if (isSelected) SurfaceVariantDark.copy(alpha = 0.18f) else SurfaceVariantDark,
                topLeft = Offset(xLeft, rectTop),
                size = Size(rodWidth, rodHeight),
                cornerRadius = CornerRadius(4f * scale, 4f * scale)
            )

            drawRoundRect(
                color = if (isSelected) PrimaryDark else OutlineVariantDark,
                topLeft = Offset(xLeft, rectTop),
                size = Size(rodWidth, rodHeight),
                cornerRadius = CornerRadius(4f * scale, 4f * scale),
                style = Stroke(width = if (isSelected) 2.5f else 1.5f)
            )

            if (abs(rod.distributedLoad) > 1e-9) {
                drawDistributedLoad(
                    textMeasurer = textMeasurer,
                    load = rod.distributedLoad,
                    xLeft = xLeft,
                    xRight = xRight,
                    centerY = centerY,
                    rectTop = rectTop,
                    rodHeight = rodHeight,
                    scale = scale,
                    textStyle = scaledMonoStyle
                )
            }

            val centerX = (xLeft + xRight) / 2f
            val circleRadius = (11f * scale).coerceIn(8f, 20f)
            drawCircle(
                color = Color(0xFF242429),
                radius = circleRadius,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = OutlineVariantDark,
                radius = circleRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            val rodNumberText = textMeasurer.measure("${rodIdx + 1}", scaledMonoStyle.copy(color = OnSurfaceDark))
            drawText(
                textLayoutResult = rodNumberText,
                topLeft = Offset(centerX - rodNumberText.size.width / 2f, centerY - rodNumberText.size.height / 2f)
            )

            drawLine(
                color = OnSurfaceVariantDark,
                start = Offset(xLeft, commonDimY),
                end = Offset(xRight, commonDimY),
                strokeWidth = 1f
            )
            drawLine(color = OnSurfaceVariantDark, start = Offset(xLeft, commonDimY - notchLen), end = Offset(xLeft, commonDimY + notchLen), strokeWidth = 1f)
            drawLine(color = OnSurfaceVariantDark, start = Offset(xRight, commonDimY - notchLen), end = Offset(xRight, commonDimY + notchLen), strokeWidth = 1f)

            val lengthText = textMeasurer.measure("L = ${rod.length}", scaledMonoStyle.copy(color = OnSurfaceVariantDark))
            drawText(
                textLayoutResult = lengthText,
                topLeft = Offset(centerX - lengthText.size.width / 2f, commonDimY + 3f * scale)
            )
        }

        for (nodeIdx in 0 until totalNodes) {
            val nodeX = viewportState.worldToScreenX(construction.nodePositionOnAxis(nodeIdx), canvasWidth, totalLength)

            var rodBottomAtNode = centerY
            if (nodeIdx > 0 && nodeIdx - 1 < construction.rods.size) {
                val h = ((construction.rods[nodeIdx - 1].crossSectionArea * 32.0).toFloat() * scale).coerceIn(16f, 220f)
                rodBottomAtNode = maxOf(rodBottomAtNode, centerY + h / 2f)
            }
            if (nodeIdx < construction.rods.size) {
                val h = ((construction.rods[nodeIdx].crossSectionArea * 32.0).toFloat() * scale).coerceIn(16f, 220f)
                rodBottomAtNode = maxOf(rodBottomAtNode, centerY + h / 2f)
            }

            if (commonDimY > rodBottomAtNode) {
                drawLine(
                    color = OnSurfaceVariantDark,
                    start = Offset(nodeX, rodBottomAtNode),
                    end = Offset(nodeX, commonDimY),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }
        }

        for (fixedNodeIdx in construction.fixedNodes) {
            if (fixedNodeIdx in 0 until totalNodes) {
                val nodeX = viewportState.worldToScreenX(construction.nodePositionOnAxis(fixedNodeIdx), canvasWidth, totalLength)
                val isLeftEdge = fixedNodeIdx == 0
                val isRightEdge = fixedNodeIdx == totalNodes - 1
                drawFixedSupport(nodeX, centerY, isLeftEdge, isRightEdge, extraColors.supportHatch, scale)
            }
        }

        for (nodeIdx in construction.nodeForces.indices) {
            val force = construction.nodeForces[nodeIdx]
            if (abs(force) > 1e-9) {
                val nodeX = viewportState.worldToScreenX(construction.nodePositionOnAxis(nodeIdx), canvasWidth, totalLength)
                drawNodalForce(textMeasurer, force, nodeX, centerY, scale, scaledMonoStyle)
            }
        }

        val squareSize = (20f * scale).coerceIn(14f, 36f)
        for (nodeIdx in 0 until totalNodes) {
            val nodeX = viewportState.worldToScreenX(construction.nodePositionOnAxis(nodeIdx), canvasWidth, totalLength)
            val isSelected = selectedNodeIndex == nodeIdx

            val nodeCornerRadius = CornerRadius((2.5f * scale).coerceIn(1.5f, 3.5f), (2.5f * scale).coerceIn(1.5f, 3.5f))

            drawRoundRect(
                color = if (isSelected) PrimaryDark else extraColors.glassChip,
                topLeft = Offset(nodeX - squareSize / 2f, centerY - squareSize / 2f),
                size = Size(squareSize, squareSize),
                cornerRadius = nodeCornerRadius
            )
            drawRoundRect(
                color = if (isSelected) PrimaryDark else OutlineVariantDark,
                topLeft = Offset(nodeX - squareSize / 2f, centerY - squareSize / 2f),
                size = Size(squareSize, squareSize),
                cornerRadius = nodeCornerRadius,
                style = Stroke(width = if (isSelected) 2f else 1.2f)
            )

            val nodeNumberText = textMeasurer.measure(
                text = "${nodeIdx + 1}",
                style = scaledMonoStyle.copy(color = if (isSelected) OnPrimaryDark else OnSurfaceDark)
            )
            drawText(
                textLayoutResult = nodeNumberText,
                topLeft = Offset(nodeX - nodeNumberText.size.width / 2f, centerY - nodeNumberText.size.height / 2f)
            )
        }

        if (construction.rods.isNotEmpty()) {
            val firstNodeX = viewportState.worldToScreenX(0.0, canvasWidth, totalLength)
            val buttonLeftX = firstNodeX - (36f * scale).coerceIn(24f, 60f)
            drawAddButton(buttonLeftX, centerY)

            val lastNodeX = viewportState.worldToScreenX(totalLength, canvasWidth, totalLength)
            val buttonRightX = lastNodeX + (36f * scale).coerceIn(24f, 60f)
            drawAddButton(buttonRightX, centerY)
        } else {
            drawAddButton(canvasWidth / 2f + viewportState.panX, centerY)
        }
    }
}

private fun DrawScope.drawDotGrid(dotsColor: Color, width: Float, height: Float, panX: Float, panY: Float) {
    val step = 32f
    val startX = (panX % step)
    val startY = (panY % step)

    var currentX = startX - step
    while (currentX < width + step) {
        var currentY = startY - step
        while (currentY < height + step) {
            drawCircle(
                color = dotsColor,
                radius = 1.2f,
                center = Offset(currentX, currentY)
            )
            currentY += step
        }
        currentX += step
    }
}

private fun DrawScope.drawFixedSupport(
    nodeX: Float,
    centerY: Float,
    isLeft: Boolean,
    isRight: Boolean,
    hatchColor: Color,
    scale: Float
) {
    val barHeight = (84f * scale).coerceIn(40f, 220f)
    val hatchLen = (9f * scale).coerceIn(6f, 20f)
    val hatchStep = (7f * scale).coerceIn(5f, 16f)

    drawLine(
        color = hatchColor,
        start = Offset(nodeX, centerY - barHeight / 2f),
        end = Offset(nodeX, centerY + barHeight / 2f),
        strokeWidth = 2.5f
    )

    val direction = if (isLeft) -1f else if (isRight) 1f else 0f
    val directions = if (direction == 0f) listOf(-1f, 1f) else listOf(direction)

    for (dir in directions) {
        var currentY = centerY - barHeight / 2f + 4f
        while (currentY <= centerY + barHeight / 2f - 4f) {
            drawLine(
                color = hatchColor,
                start = Offset(nodeX, currentY),
                end = Offset(nodeX + dir * hatchLen, currentY + dir * hatchLen),
                strokeWidth = 1.5f
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
    textStyle: TextStyle
) {
    val arrowLen = (52f * scale).coerceIn(30f, 120f)
    val direction = if (force > 0) 1f else -1f
    val startX = nodeX - direction * arrowLen
    val endX = nodeX

    drawLine(
        color = PrimaryDark,
        start = Offset(startX, centerY),
        end = Offset(endX, centerY),
        strokeWidth = 2.5f
    )

    val tipLen = (9f * scale).coerceIn(6f, 16f)
    val arrowPath = Path().apply {
        moveTo(endX, centerY)
        lineTo(endX - direction * tipLen, centerY - 5f * scale)
        lineTo(endX - direction * tipLen, centerY + 5f * scale)
        close()
    }
    drawPath(arrowPath, color = PrimaryDark)

    val text = textMeasurer.measure("F = $force", textStyle.copy(color = PrimaryDark))
    drawText(
        textLayoutResult = text,
        topLeft = Offset(
            startX - (if (direction < 0) 0f else text.size.width.toFloat()),
            centerY - text.size.height - 4f * scale
        )
    )
}

private fun DrawScope.drawDistributedLoad(
    textMeasurer: TextMeasurer,
    load: Double,
    xLeft: Float,
    xRight: Float,
    centerY: Float,
    rectTop: Float,
    rodHeight: Float,
    scale: Float,
    textStyle: TextStyle
) {
    val direction = if (load > 0) 1f else -1f
    val rodWidth = (xRight - xLeft).coerceAtLeast(1f)

    val yRows = when {
        rodHeight < 36f * scale -> listOf(centerY)
        rodHeight < 72f * scale -> listOf(centerY - rodHeight * 0.22f, centerY + rodHeight * 0.22f)
        else -> listOf(centerY - rodHeight * 0.28f, centerY, centerY + rodHeight * 0.28f)
    }

    val padX = (10f * scale).coerceIn(6f, 20f).coerceAtMost(rodWidth * 0.2f)
    val availableWidth = rodWidth - 2 * padX

    val arrowLen = (16f * scale).coerceIn(10f, 28f).coerceAtMost(availableWidth)
    val gap = (10f * scale).coerceIn(6f, 16f)
    val tipLen = (5f * scale).coerceIn(3.5f, 9f).coerceAtMost(arrowLen * 0.5f)
    val tipWing = (3f * scale).coerceIn(2f, 5.5f)

    val count = ((availableWidth + gap) / (arrowLen + gap)).toInt().coerceIn(1, 15)
    val step = availableWidth / count

    for (y in yRows) {
        for (i in 0 until count) {
            val intervalCenter = (xLeft + padX) + (i + 0.5f) * step
            val arrowStartX = intervalCenter - direction * (arrowLen / 2f)
            val arrowEndX = intervalCenter + direction * (arrowLen / 2f)

            drawLine(
                color = PrimaryDark,
                start = Offset(arrowStartX, y),
                end = Offset(arrowEndX, y),
                strokeWidth = 1.6f
            )

            val arrowPath = Path().apply {
                moveTo(arrowEndX, y)
                lineTo(arrowEndX - direction * tipLen, y - tipWing)
                lineTo(arrowEndX - direction * tipLen, y + tipWing)
                close()
            }
            drawPath(arrowPath, color = PrimaryDark)
        }
    }

    val text = textMeasurer.measure("q = $load", textStyle.copy(color = PrimaryDark))
    val textX = (xLeft + xRight) / 2f - text.size.width / 2f
    val textY = rectTop - text.size.height - (4f * scale).coerceIn(2f, 8f)
    drawText(
        textLayoutResult = text,
        topLeft = Offset(textX, textY)
    )
}

private fun DrawScope.drawAddButton(buttonCenterX: Float, centerY: Float) {
    val radius = 18f
    drawCircle(
        color = SurfaceVariantDark,
        radius = radius,
        center = Offset(buttonCenterX, centerY)
    )
    drawCircle(
        color = OutlineVariantDark,
        radius = radius,
        center = Offset(buttonCenterX, centerY),
        style = Stroke(width = 1.5f)
    )

    val plusLen = 7f
    drawLine(
        color = PrimaryDark,
        start = Offset(buttonCenterX - plusLen, centerY),
        end = Offset(buttonCenterX + plusLen, centerY),
        strokeWidth = 2f
    )
    drawLine(
        color = PrimaryDark,
        start = Offset(buttonCenterX, centerY - plusLen),
        end = Offset(buttonCenterX, centerY + plusLen),
        strokeWidth = 2f
    )
}
