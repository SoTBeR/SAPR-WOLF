package ru.sotber.sapr_wolf.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.SectionResult
import ru.sotber.sapr_wolf.model.nodeCount
import ru.sotber.sapr_wolf.postprocessor.calculateSection
import ru.sotber.sapr_wolf.ui.ViewportState
import ru.sotber.sapr_wolf.ui.formatCalculationValue
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

enum class EpureComponent(val symbol: String) {
    AxialForce("Nx"),
    NormalStress("σx"),
    Displacement("Ux"),
}

enum class EpureDisplayMode {
    All,
    AxialForce,
    NormalStress,
    Displacement,
    None,
}

fun EpureDisplayMode.toComponents(): List<EpureComponent> = when (this) {
    EpureDisplayMode.All -> listOf(
        EpureComponent.AxialForce,
        EpureComponent.NormalStress,
        EpureComponent.Displacement,
    )
    EpureDisplayMode.AxialForce -> listOf(EpureComponent.AxialForce)
    EpureDisplayMode.NormalStress -> listOf(EpureComponent.NormalStress)
    EpureDisplayMode.Displacement -> listOf(EpureComponent.Displacement)
    EpureDisplayMode.None -> emptyList()
}

private object EpureDefaults {
    const val BASE_TOP_OFFSET_BELOW_DIM = 74f
    const val BASE_HALF_HEIGHT = 48f
    const val BASE_BAND_SPACING = 132f
    const val BASE_HATCH_STEP = 5f
    const val BASE_LABEL_INSET_X = 6f
    const val BASE_LABEL_GAP_Y = 3f
    const val BASE_AXIS_STROKE = 1.4f
    const val BASE_CONTOUR_STROKE = 1.8f
    const val BASE_HATCH_STROKE = 1.15f
    const val BASE_GUIDE_STROKE = 0.8f
    const val PARABOLA_SEGMENTS = 24
    const val ZERO_EPS = 1e-9
}

internal data class EpurePalette(
    val axis: Color,
    val guide: Color,
    val stroke: Color,
    val fill: Color,
    val labelText: Color,
    val inspectionMarker: Color,
)

internal fun DrawScope.drawEpures(
    construction: Construction,
    displacements: DoubleArray,
    displayMode: EpureDisplayMode,
    viewportState: ViewportState,
    canvasWidth: Float,
    totalLength: Double,
    commonDimY: Float,
    scale: Float,
    palette: EpurePalette,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    inspectedRodIndex: Int?,
    inspectedLocalX: Double?,
) {
    val components = displayMode.toComponents()
    if (components.isEmpty() || construction.rods.isEmpty()) return

    val halfHeight = EpureDefaults.BASE_HALF_HEIGHT * scale
    val bandSpacing = EpureDefaults.BASE_BAND_SPACING * scale
    val firstAxisY = commonDimY + EpureDefaults.BASE_TOP_OFFSET_BELOW_DIM * scale
    val lastAxisY = firstAxisY + (components.size - 1) * bandSpacing

    val leftEdgeX = viewportState.worldToScreenX(0.0, canvasWidth, totalLength)
    val rightEdgeX = viewportState.worldToScreenX(totalLength, canvasWidth, totalLength)

    val guideDash = PathEffect.dashPathEffect(floatArrayOf(4f * scale, 4f * scale), 0f)
    val guideStroke = (EpureDefaults.BASE_GUIDE_STROKE * scale).coerceAtLeast(0.6f)
    val guideTopY = commonDimY + 18f * scale
    val guideBottomY = lastAxisY + halfHeight + 16f * scale

    for (nodeIndex in 0 until construction.nodeCount) {
        val nodeX = viewportState.worldToScreenX(
            worldX = construction.displayNodePositionOnAxis(nodeIndex),
            canvasWidth = canvasWidth,
            totalConstructionLength = totalLength,
        )
        drawLine(
            color = palette.guide,
            start = Offset(nodeX, guideTopY),
            end = Offset(nodeX, guideBottomY),
            strokeWidth = guideStroke,
            pathEffect = guideDash,
        )
    }

    components.forEachIndexed { bandIndex, component ->
        val axisY = firstAxisY + bandIndex * bandSpacing
        drawSingleComponentEpure(
            construction = construction,
            displacements = displacements,
            component = component,
            axisY = axisY,
            maxHalfHeightPx = halfHeight,
            leftEdgeX = leftEdgeX,
            rightEdgeX = rightEdgeX,
            viewportState = viewportState,
            canvasWidth = canvasWidth,
            totalLength = totalLength,
            scale = scale,
            palette = palette,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
        )
    }

    if (inspectedRodIndex != null && inspectedLocalX != null && inspectedRodIndex in construction.rods.indices) {
        val rod = construction.rods[inspectedRodIndex]
        val clampedX = inspectedLocalX.coerceIn(0.0, rod.length)
        val worldX = construction.displaySectionWorldX(inspectedRodIndex, clampedX)
        val markerX = viewportState.worldToScreenX(worldX, canvasWidth, totalLength)
        drawLine(
            color = palette.inspectionMarker,
            start = Offset(markerX, guideTopY),
            end = Offset(markerX, guideBottomY),
            strokeWidth = (1.5f * scale).coerceAtLeast(1f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * scale, 3f * scale), 0f),
        )
    }
}

private fun DrawScope.drawSingleComponentEpure(
    construction: Construction,
    displacements: DoubleArray,
    component: EpureComponent,
    axisY: Float,
    maxHalfHeightPx: Float,
    leftEdgeX: Float,
    rightEdgeX: Float,
    viewportState: ViewportState,
    canvasWidth: Float,
    totalLength: Double,
    scale: Float,
    palette: EpurePalette,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
) {
    val rodSamples = construction.rods.indices.map { rodIndex ->
        val rod = construction.rods[rodIndex]
        val segments = if (component == EpureComponent.Displacement && abs(rod.distributedLoad) > EpureDefaults.ZERO_EPS) {
            EpureDefaults.PARABOLA_SEGMENTS
        } else {
            1
        }
        List(segments + 1) { step ->
            val fraction = step.toDouble() / segments.toDouble()
            val localX = rod.length * fraction
            val section = calculateSection(construction, displacements, rodIndex, localX)
            localX to component.extractValue(section)
        }
    }

    var maxAbsValue = 0.0
    for (samples in rodSamples) {
        for ((_, value) in samples) {
            maxAbsValue = max(maxAbsValue, abs(value))
        }
    }

    val epureScale = if (maxAbsValue > EpureDefaults.ZERO_EPS) {
        maxHalfHeightPx / maxAbsValue
    } else {
        0.0
    }

    val titleLayout = textMeasurer.measure(
        text = component.symbol,
        style = textStyle.copy(color = palette.labelText),
    )
    drawText(
        textLayoutResult = titleLayout,
        topLeft = Offset(
            x = leftEdgeX - titleLayout.size.width - 14f * scale,
            y = axisY - titleLayout.size.height / 2f,
        ),
    )

    val axisStroke = (EpureDefaults.BASE_AXIS_STROKE * scale).coerceAtLeast(0.8f)
    drawLine(
        color = palette.axis,
        start = Offset(leftEdgeX, axisY),
        end = Offset(rightEdgeX, axisY),
        strokeWidth = axisStroke,
    )

    val contourStroke = (EpureDefaults.BASE_CONTOUR_STROKE * scale).coerceAtLeast(1f)
    val hatchStroke = (EpureDefaults.BASE_HATCH_STROKE * scale).coerceAtLeast(0.8f)
    val hatchStepPx = (EpureDefaults.BASE_HATCH_STEP * scale).coerceAtLeast(2.5f)

    for (rodIndex in construction.rods.indices) {
        val rod = construction.rods[rodIndex]
        val samples = rodSamples[rodIndex]
        val rodStartWorldX = construction.displayNodePositionOnAxis(rodIndex)
        val xLeft = viewportState.worldToScreenX(rodStartWorldX, canvasWidth, totalLength)
        val xRight = viewportState.worldToScreenX(rodStartWorldX + displayRodLength(rod), canvasWidth, totalLength)
        val rodScreenWidth = xRight - xLeft
        if (rodScreenWidth <= 0.5f) continue

        val screenPoints = samples.map { (localX, value) ->
            val screenX = viewportState.worldToScreenX(
                construction.displaySectionWorldX(rodIndex, localX),
                canvasWidth,
                totalLength,
            )
            val screenY = axisY - (value * epureScale).toFloat()
            Triple(screenX, screenY, value)
        }

        if (epureScale > 0.0) {
            val fillPath = Path().apply {
                moveTo(xLeft, axisY)
                for ((sx, sy, _) in screenPoints) {
                    lineTo(sx, sy)
                }
                lineTo(xRight, axisY)
                close()
            }
            drawPath(path = fillPath, color = palette.fill)

            val firstHatchIndex = ceil(((xLeft - leftEdgeX) / hatchStepPx).toDouble()).toInt()
            var hatchIndex = firstHatchIndex
            while (true) {
                val hatchX = leftEdgeX + hatchIndex * hatchStepPx
                if (hatchX >= xRight - 0.5f) break
                if (hatchX > xLeft + 0.5f) {
                    val fraction = ((hatchX - xLeft) / rodScreenWidth).coerceIn(0f, 1f).toDouble()
                    val localX = rod.length * fraction
                    val value = component.extractValue(
                        calculateSection(construction, displacements, rodIndex, localX)
                    )
                    if (abs(value) > EpureDefaults.ZERO_EPS) {
                        val yVal = axisY - (value * epureScale).toFloat()
                        drawLine(
                            color = palette.stroke.copy(alpha = 0.75f),
                            start = Offset(hatchX, axisY),
                            end = Offset(hatchX, yVal),
                            strokeWidth = hatchStroke,
                        )
                    }
                }
                hatchIndex++
            }
        }

        val contourPath = Path().apply {
            moveTo(xLeft, axisY)
            for ((sx, sy, _) in screenPoints) {
                lineTo(sx, sy)
            }
            lineTo(xRight, axisY)
        }
        drawPath(
            path = contourPath,
            color = palette.stroke,
            style = Stroke(width = contourStroke),
        )

        val startValue = samples.first().second
        val endValue = samples.last().second
        val startY = screenPoints.first().second
        val endY = screenPoints.last().second
        val isConstant = abs(startValue - endValue) <= EpureDefaults.ZERO_EPS &&
            !(component == EpureComponent.Displacement && abs(rod.distributedLoad) > EpureDefaults.ZERO_EPS)

        if (isConstant) {
            drawEpureValueLabel(
                value = startValue,
                anchorX = (xLeft + xRight) / 2f,
                anchorY = startY,
                axisY = axisY,
                alignMode = LabelAlignMode.Center,
                scale = scale,
                palette = palette,
                textMeasurer = textMeasurer,
                textStyle = textStyle,
            )
        } else {
            drawEpureValueLabel(
                value = startValue,
                anchorX = xLeft + EpureDefaults.BASE_LABEL_INSET_X * scale,
                anchorY = startY,
                axisY = axisY,
                alignMode = LabelAlignMode.Left,
                scale = scale,
                palette = palette,
                textMeasurer = textMeasurer,
                textStyle = textStyle,
            )
            drawEpureValueLabel(
                value = endValue,
                anchorX = xRight - EpureDefaults.BASE_LABEL_INSET_X * scale,
                anchorY = endY,
                axisY = axisY,
                alignMode = LabelAlignMode.Right,
                scale = scale,
                palette = palette,
                textMeasurer = textMeasurer,
                textStyle = textStyle,
            )
        }
    }
}

private enum class LabelAlignMode { Left, Center, Right }

private fun DrawScope.drawEpureValueLabel(
    value: Double,
    anchorX: Float,
    anchorY: Float,
    axisY: Float,
    alignMode: LabelAlignMode,
    scale: Float,
    palette: EpurePalette,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
) {
    val labelText = formatCalculationValue(abs(value))
    val layout = textMeasurer.measure(
        text = labelText,
        style = textStyle.copy(color = palette.labelText),
    )
    val textX = when (alignMode) {
        LabelAlignMode.Left -> anchorX
        LabelAlignMode.Center -> anchorX - layout.size.width / 2f
        LabelAlignMode.Right -> anchorX - layout.size.width
    }
    val gapY = EpureDefaults.BASE_LABEL_GAP_Y * scale
    val textY = if (anchorY <= axisY) {
        anchorY - layout.size.height - gapY
    } else {
        anchorY + gapY
    }

    drawText(
        textLayoutResult = layout,
        topLeft = Offset(textX, textY),
    )
}

internal fun EpureComponent.extractValue(section: SectionResult): Double = when (this) {
    EpureComponent.AxialForce -> section.axialForce
    EpureComponent.NormalStress -> section.normalStress
    EpureComponent.Displacement -> section.displacement
}
