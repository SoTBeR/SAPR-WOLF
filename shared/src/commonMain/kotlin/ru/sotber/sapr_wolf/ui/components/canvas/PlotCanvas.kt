package ru.sotber.sapr_wolf.ui.components.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.postprocessor.calculateSection
import ru.sotber.sapr_wolf.ui.formatCalculationValue
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors
import ru.sotber.sapr_wolf.ui.theme.MonoSmallTextStyle
import ru.sotber.sapr_wolf.ui.theme.labelMediumBold
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.postprocessor_plot_title
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val PLOT_SAMPLES = 40
private const val PLOT_ZERO_EPS = 1e-9
private const val PLOT_RELATIVE_EPS = 1e-12

@Composable
fun PlotCanvas(
    construction: Construction,
    displacements: DoubleArray,
    rodIndex: Int,
    component: EpureComponent,
    inspectedLocalX: Double?,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 112.dp,
) {
    if (rodIndex !in construction.rods.indices) return
    val rod = construction.rods[rodIndex]
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MaterialTheme.colorScheme
    val extraColors = MaterialThemeExtraColors

    val samples = remember(construction, displacements, rodIndex, component) {
        List(PLOT_SAMPLES + 1) { index ->
            val fraction = index.toDouble() / PLOT_SAMPLES.toDouble()
            val localX = rod.length * fraction
            val section = calculateSection(construction, displacements, rodIndex, localX)
            localX to component.extractValue(section)
        }
    }

    val inspectedValue = remember(construction, displacements, rodIndex, component, inspectedLocalX) {
        inspectedLocalX?.let { x ->
            component.extractValue(
                calculateSection(construction, displacements, rodIndex, x.coerceIn(0.0, rod.length))
            )
        }
    }

    val rawMin = samples.minOf { it.second }
    val rawMax = samples.maxOf { it.second }
    val valueScale = max(1.0, max(abs(rawMin), abs(rawMax)))
    val isConstantCurve = (rawMax - rawMin) <= PLOT_RELATIVE_EPS * valueScale
    val isZeroCurve = isConstantCurve && abs(rawMax) <= PLOT_ZERO_EPS
    val axisMin: Double
    val axisMax: Double
    when {
        isZeroCurve -> {
            axisMin = -1.0
            axisMax = 1.0
        }
        isConstantCurve -> {
            axisMin = min(0.0, rawMin)
            axisMax = max(0.0, rawMax)
        }
        else -> {
            axisMin = rawMin
            axisMax = rawMax
        }
    }
    val hasZeroInRange = axisMin <= PLOT_ZERO_EPS && axisMax >= -PLOT_ZERO_EPS

    val lineColor = when (component) {
        EpureComponent.AxialForce -> extraColors.tensionLine
        EpureComponent.NormalStress -> colorScheme.primary
        EpureComponent.Displacement -> extraColors.compressionLine
    }
    val gridColor = colorScheme.outlineVariant.copy(alpha = 0.45f)
    val axisColor = colorScheme.onSurfaceVariant
    val labelStyle = MonoSmallTextStyle.copy(
        fontSize = 10.sp,
        color = colorScheme.onSurfaceVariant,
    )

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
        ) {
            Text(
                text = stringResource(Res.string.postprocessor_plot_title, component.symbol),
                style = MaterialTheme.typography.labelMediumBold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (inspectedValue != null && inspectedLocalX != null) {
                Text(
                    text = formatCalculationValue(inspectedValue),
                    style = MonoSmallTextStyle.copy(color = colorScheme.primary),
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            val topPad = 10f
            val bottomPad = 22f
            val rightPad = 10f

            val plotTop = topPad
            val plotBottom = (size.height - bottomPad).coerceAtLeast(plotTop + 10f)
            val plotHeight = plotBottom - plotTop

            fun mapY(value: Double): Float {
                val frac = ((value - axisMin) / (axisMax - axisMin)).coerceIn(0.0, 1.0)
                return plotBottom - (frac * plotHeight).toFloat()
            }

            val zeroY = mapY(0.0)

            val yTickLayouts = buildList {
                if (isZeroCurve) {
                    val zeroLayout = textMeasurer.measure(formatCalculationValue(0.0), labelStyle)
                    add(zeroLayout to zeroY)
                } else {
                    val maxLayout = textMeasurer.measure(formatCalculationValue(axisMax), labelStyle)
                    val minLayout = textMeasurer.measure(formatCalculationValue(axisMin), labelStyle)
                    add(maxLayout to plotTop)
                    if (axisMin < -PLOT_ZERO_EPS && axisMax > PLOT_ZERO_EPS) {
                        val zeroLayout = textMeasurer.measure(formatCalculationValue(0.0), labelStyle)
                        val minClearance = zeroLayout.size.height + 2f
                        if (zeroY - plotTop >= minClearance && plotBottom - zeroY >= minClearance) {
                            add(zeroLayout to zeroY)
                        }
                    }
                    add(minLayout to plotBottom)
                }
            }

            val maxTickWidth = yTickLayouts.maxOfOrNull { it.first.size.width.toFloat() } ?: 0f
            val leftPad = max(42f, maxTickWidth + 6f)

            val plotLeft = leftPad
            val plotRight = (size.width - rightPad).coerceAtLeast(plotLeft + 10f)
            val plotWidth = plotRight - plotLeft

            fun mapX(localX: Double): Float {
                val frac = if (rod.length > PLOT_ZERO_EPS) (localX / rod.length).coerceIn(0.0, 1.0) else 0.0
                return plotLeft + (frac * plotWidth).toFloat()
            }

            val gridDash = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
            for (i in 0..2) {
                val frac = i / 2f
                val gx = plotLeft + frac * plotWidth
                drawLine(
                    color = gridColor,
                    start = Offset(gx, plotTop),
                    end = Offset(gx, plotBottom),
                    strokeWidth = 0.8f,
                    pathEffect = gridDash,
                )
                val gy = plotTop + frac * plotHeight
                drawLine(
                    color = gridColor,
                    start = Offset(plotLeft, gy),
                    end = Offset(plotRight, gy),
                    strokeWidth = 0.8f,
                    pathEffect = gridDash,
                )
            }

            drawLine(
                color = axisColor,
                start = Offset(plotLeft, plotTop),
                end = Offset(plotLeft, plotBottom),
                strokeWidth = 1.2f,
            )
            drawLine(
                color = axisColor,
                start = Offset(plotLeft, plotBottom),
                end = Offset(plotRight, plotBottom),
                strokeWidth = 1.2f,
            )

            if (hasZeroInRange) {
                drawLine(
                    color = axisColor.copy(alpha = 0.7f),
                    start = Offset(plotLeft, zeroY),
                    end = Offset(plotRight, zeroY),
                    strokeWidth = 1f,
                )
            }

            val fillPath = Path().apply {
                moveTo(mapX(samples.first().first), zeroY)
                for ((xVal, yVal) in samples) {
                    lineTo(mapX(xVal), mapY(yVal))
                }
                lineTo(mapX(samples.last().first), zeroY)
                close()
            }
            drawPath(path = fillPath, color = lineColor.copy(alpha = 0.12f))

            val curvePath = Path().apply {
                samples.forEachIndexed { index, (xVal, yVal) ->
                    val px = mapX(xVal)
                    val py = mapY(yVal)
                    if (index == 0) moveTo(px, py) else lineTo(px, py)
                }
            }
            drawPath(
                path = curvePath,
                color = lineColor,
                style = Stroke(width = 2f),
            )

            if (inspectedLocalX != null && inspectedValue != null) {
                val clampedX = inspectedLocalX.coerceIn(0.0, rod.length)
                val mx = mapX(clampedX)
                val my = mapY(inspectedValue)
                drawLine(
                    color = colorScheme.primary.copy(alpha = 0.7f),
                    start = Offset(mx, plotTop),
                    end = Offset(mx, plotBottom),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f),
                )
                drawCircle(
                    color = colorScheme.primary,
                    radius = 3.5f,
                    center = Offset(mx, my),
                )
            }

            for ((layout, tickY) in yTickLayouts) {
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = (plotLeft - layout.size.width - 4f).coerceAtLeast(0f),
                        y = (tickY - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height),
                    ),
                )
            }

            val xTicks = listOf(
                0.0 to plotLeft,
                rod.length / 2.0 to (plotLeft + plotRight) / 2f,
                rod.length to plotRight,
            )
            for ((index, pair) in xTicks.withIndex()) {
                val (tickVal, tickX) = pair
                val layout = textMeasurer.measure(formatCalculationValue(tickVal), labelStyle)
                val tx = when (index) {
                    0 -> tickX
                    2 -> (tickX - layout.size.width).coerceAtLeast(plotLeft)
                    else -> tickX - layout.size.width / 2f
                }
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x = tx, y = plotBottom + 4f),
                )
            }
        }
    }
}
