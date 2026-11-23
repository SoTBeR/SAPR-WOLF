package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.model.SectionResult
import ru.sotber.sapr_wolf.model.nodeCount
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private object SvgDefaults {
    const val SVG_WIDTH = 896.0
    const val MIN_LEFT_MARGIN = 76.0
    const val MIN_RIGHT_MARGIN = 48.0
    const val TOP_PADDING = 22.0
    const val BOTTOM_PADDING = 24.0

    const val DOT_GRID_STEP = 32.0
    const val DOT_GRID_RADIUS = 1.2

    const val MAX_WORLD_ROD_LENGTH = (500.0 * 1.5) / 80.0
    const val BASE_ROD_HEIGHT_PER_AREA = 32.0
    const val MIN_BASE_ROD_HEIGHT = 25.0
    const val MAX_BASE_ROD_HEIGHT = 500.0
    const val MAX_SVG_ROD_HEIGHT = 140.0
    const val MIN_SVG_ROD_HEIGHT = 24.0

    const val ROD_STROKE_WIDTH = 1.4
    const val ROD_OVERLOAD_STROKE_WIDTH = 2.2
    const val ROD_BADGE_RADIUS = 11.0

    const val DIMENSION_OFFSET_Y = 28.0
    const val DIMENSION_NOTCH_HALF_LEN = 4.0
    const val DIMENSION_TEXT_GAP_Y = 5.0
    const val LINE_STROKE_WIDTH = 1.0

    const val NODE_SIZE = 20.0
    const val NODE_CORNER_RADIUS = 3.0
    const val NODE_STROKE_WIDTH = 1.2

    const val SUPPORT_MIN_HEIGHT = 84.0
    const val SUPPORT_EXTRA_MARGIN = 20.0
    const val SUPPORT_WALL_STROKE = 2.4
    const val SUPPORT_HATCH_LEN = 9.0
    const val SUPPORT_HATCH_STEP = 7.0
    const val SUPPORT_HATCH_INSET = 4.0
    const val SUPPORT_HATCH_STROKE = 1.4

    const val NODAL_FORCE_ARROW_LEN = 52.0
    const val NODAL_FORCE_SHAFT_STROKE = 5.0
    const val NODAL_FORCE_TIP_LEN = 9.0
    const val NODAL_FORCE_TIP_HALF_HEIGHT = 9.0
    const val NODAL_FORCE_TEXT_GAP_Y = 12.0

    const val Q_PAD_X = 12.0
    const val Q_ARROW_LEN = 16.0
    const val Q_ARROW_GAP = 10.0
    const val Q_TIP_LEN = 5.0
    const val Q_TIP_WING = 3.0
    const val Q_SHAFT_STROKE = 1.5
    const val Q_TEXT_GAP_Y = 5.0
    const val MAX_Q_ARROWS_PER_ROW = 24

    const val EPURE_TOP_OFFSET_BELOW_DIM = 78.0
    const val EPURE_HALF_HEIGHT = 48.0
    const val EPURE_BAND_SPACING = 132.0
    const val EPURE_HATCH_STEP = 5.0
    const val EPURE_LABEL_INSET_X = 6.0
    const val EPURE_LABEL_GAP_Y = 4.0
    const val EPURE_LABEL_CURVE_CLEARANCE_X = 2.0
    const val EPURE_AXIS_STROKE = 1.4
    const val EPURE_CONTOUR_STROKE = 1.8
    const val EPURE_HATCH_STROKE = 1.15
    const val EPURE_GUIDE_STROKE = 1.0
    const val EPURE_PARABOLA_SEGMENTS = 24

    const val FONT_SIZE = 12.0
    const val CHAR_WIDTH_ESTIMATE = 7.2
    const val TEXT_HEIGHT_ESTIMATE = 13.0
    const val ZERO_EPS = 1e-9
    const val FLOAT_EPSILON = 1e-12
}

private object SvgPalette {
    // Matches LightColorScheme + LightSaprExtraColors from Theme.kt / Color.kt
    const val CANVAS_BASE = "#f8f8fa"
    const val CANVAS_DOTS = "rgba(0, 0, 0, 0.09)"
    const val AXIS = "rgba(0, 0, 0, 0.16)"
    const val ROD_FILL = "rgba(0, 0, 0, 0.04)"
    const val ROD_BORDER = "rgba(0, 0, 0, 0.16)"
    const val ROD_OVERLOAD_FILL = "rgba(217, 45, 32, 0.22)"
    const val ROD_OVERLOAD_BORDER = "#d92d20"
    const val NODE_FILL = "rgba(255, 255, 255, 0.60)"
    const val NODE_BORDER = "rgba(0, 0, 0, 0.16)"
    const val ON_NODE = "#111113"
    const val FORCE_AND_LOAD = "#111113"
    const val DIMENSION = "#6e6e73"
    const val SUPPORT_HATCH = "#6e6e73"

    const val EPURE_AXIS = "#111113"
    const val EPURE_GUIDE = DIMENSION
    const val EPURE_STROKE = "#111113"
    const val EPURE_HATCH = "rgba(17, 17, 19, 0.75)"
    const val EPURE_FILL = "rgba(17, 17, 19, 0.05)"
    const val EPURE_LABEL = "#111113"
}

private enum class SvgEpureComponent(val symbol: String) {
    AxialForce("Nx"),
    NormalStress("σx"),
    Displacement("Ux"),
}

private fun SvgEpureComponent.extractValue(section: SectionResult): Double = when (this) {
    SvgEpureComponent.AxialForce -> section.axialForce
    SvgEpureComponent.NormalStress -> section.normalStress
    SvgEpureComponent.Displacement -> section.displacement
}

private enum class SvgLabelAlign { Left, Center, Right }

internal fun buildConstructionAndEpuresSvg(
    construction: Construction,
    displacements: DoubleArray,
    overloadedRodIndices: Set<Int>,
): String {
    if (construction.rods.isEmpty()) return ""

    val totalNodes = construction.nodeCount
    val displayLengths = construction.rods.map(::svgDisplayRodLength)
    val totalDisplayLength = displayLengths.sum()
    if (totalDisplayLength <= SvgDefaults.FLOAT_EPSILON) return ""

    val nodeWorldX = DoubleArray(totalNodes)
    for (i in 1 until totalNodes) {
        nodeWorldX[i] = nodeWorldX[i - 1] + displayLengths[i - 1]
    }

    val firstNodeForce = construction.nodeForces.firstOrNull() ?: 0.0
    val lastNodeForce = construction.nodeForces.getOrNull(totalNodes - 1) ?: 0.0
    val leftForceClearance = if (firstNodeForce < -SvgDefaults.FLOAT_EPSILON) {
        SvgDefaults.NODE_SIZE / 2.0 + SvgDefaults.NODAL_FORCE_ARROW_LEN + 20.0
    } else {
        0.0
    }
    val rightForceClearance = if (lastNodeForce > SvgDefaults.FLOAT_EPSILON) {
        SvgDefaults.NODE_SIZE / 2.0 + SvgDefaults.NODAL_FORCE_ARROW_LEN + 20.0
    } else {
        0.0
    }

    val leftMargin = max(SvgDefaults.MIN_LEFT_MARGIN, leftForceClearance)
    val rightMargin = max(SvgDefaults.MIN_RIGHT_MARGIN, rightForceClearance)
    val availableWidth = max(200.0, SvgDefaults.SVG_WIDTH - leftMargin - rightMargin)

    fun worldToX(worldX: Double): Double =
        leftMargin + (worldX / totalDisplayLength) * availableWidth

    val nodeX = DoubleArray(totalNodes) { idx -> worldToX(nodeWorldX[idx]) }
    val leftEdgeX = nodeX.first()
    val rightEdgeX = nodeX.last()

    val rawRodHeights = construction.rods.map(::svgBaseRodHeight)
    val maxRawRodHeight = rawRodHeights.max()
    val heightScale = if (maxRawRodHeight > SvgDefaults.MAX_SVG_ROD_HEIGHT) {
        SvgDefaults.MAX_SVG_ROD_HEIGHT / maxRawRodHeight
    } else {
        1.0
    }
    val rodHeights = rawRodHeights.map { raw ->
        (raw * heightScale).coerceAtLeast(SvgDefaults.MIN_SVG_ROD_HEIGHT)
    }
    val maxRodHeight = rodHeights.max()

    fun maxAdjacentRodHeight(nodeIdx: Int): Double {
        var result = 0.0
        if (nodeIdx - 1 in rodHeights.indices) result = max(result, rodHeights[nodeIdx - 1])
        if (nodeIdx in rodHeights.indices) result = max(result, rodHeights[nodeIdx])
        return result
    }

    val maxSupportHalfHeight = if (construction.fixedNodes.isEmpty()) {
        0.0
    } else {
        construction.fixedNodes
            .filter { it in 0 until totalNodes }
            .maxOfOrNull { idx ->
                max(SvgDefaults.SUPPORT_MIN_HEIGHT, maxAdjacentRodHeight(idx) + SvgDefaults.SUPPORT_EXTRA_MARGIN) / 2.0
            } ?: 0.0
    }

    val hasDistributedLoad = construction.rods.any { abs(it.distributedLoad) > SvgDefaults.FLOAT_EPSILON }
    val hasNodalForce = construction.nodeForces.any { abs(it) > SvgDefaults.FLOAT_EPSILON }
    val topRequiredHalf = max(
        maxRodHeight / 2.0 + if (hasDistributedLoad) 24.0 else 10.0,
        max(
            maxSupportHalfHeight + 8.0,
            if (hasNodalForce) 30.0 else 16.0,
        ),
    )

    val centerY = SvgDefaults.TOP_PADDING + topRequiredHalf
    val commonDimY = centerY + maxRodHeight / 2.0 + SvgDefaults.DIMENSION_OFFSET_Y

    val components = listOf(
        SvgEpureComponent.AxialForce,
        SvgEpureComponent.NormalStress,
        SvgEpureComponent.Displacement,
    )
    val firstAxisY = commonDimY + SvgDefaults.EPURE_TOP_OFFSET_BELOW_DIM
    val lastAxisY = firstAxisY + (components.size - 1) * SvgDefaults.EPURE_BAND_SPACING
    val guideTopY = commonDimY + 10.0
    val guideBottomY = lastAxisY + SvgDefaults.EPURE_HALF_HEIGHT + 16.0
    val svgHeight = guideBottomY + SvgDefaults.BOTTOM_PADDING

    val sb = StringBuilder()
    sb.appendLine(
        "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
            "viewBox=\"0 0 ${fmt(SvgDefaults.SVG_WIDTH)} ${fmt(svgHeight)}\" " +
            "width=\"100%\" role=\"img\" aria-label=\"Конструкция, расчётная схема и эпюры\">"
    )
    sb.appendLine("  <defs>")
    sb.appendLine(
        "    <pattern id=\"sapr-dot-grid\" width=\"${fmt(SvgDefaults.DOT_GRID_STEP)}\" " +
            "height=\"${fmt(SvgDefaults.DOT_GRID_STEP)}\" patternUnits=\"userSpaceOnUse\">"
    )
    sb.appendLine(
        "      <circle cx=\"${fmt(SvgDefaults.DOT_GRID_STEP / 2.0)}\" " +
            "cy=\"${fmt(SvgDefaults.DOT_GRID_STEP / 2.0)}\" " +
            "r=\"${fmt(SvgDefaults.DOT_GRID_RADIUS)}\" fill=\"${SvgPalette.CANVAS_DOTS}\"/>"
    )
    sb.appendLine("    </pattern>")
    sb.appendLine("  </defs>")

    // Canvas background + dot grid (matching ConstructionCanvas)
    sb.appendLine(
        "  <rect x=\"0\" y=\"0\" width=\"${fmt(SvgDefaults.SVG_WIDTH)}\" height=\"${fmt(svgHeight)}\" " +
            "rx=\"8\" fill=\"${SvgPalette.CANVAS_BASE}\"/>"
    )
    sb.appendLine(
        "  <rect x=\"0\" y=\"0\" width=\"${fmt(SvgDefaults.SVG_WIDTH)}\" height=\"${fmt(svgHeight)}\" " +
            "rx=\"8\" fill=\"url(#sapr-dot-grid)\"/>"
    )

    // Main horizontal axis
    val axisStartX = max(12.0, leftEdgeX - max(28.0, leftForceClearance - 8.0))
    val axisEndX = min(SvgDefaults.SVG_WIDTH - 12.0, rightEdgeX + max(28.0, rightForceClearance - 8.0))
    sb.appendLine(
        "  <line x1=\"${fmt(axisStartX)}\" y1=\"${fmt(centerY)}\" " +
            "x2=\"${fmt(axisEndX)}\" y2=\"${fmt(centerY)}\" " +
            "stroke=\"${SvgPalette.AXIS}\" stroke-width=\"${fmt(SvgDefaults.LINE_STROKE_WIDTH)}\"/>"
    )

    // Rods, distributed loads, rod badges, and length dimensions
    sb.appendLine("  <g class=\"rods\">")
    for (rodIdx in construction.rods.indices) {
        val rod = construction.rods[rodIdx]
        val xLeft = nodeX[rodIdx]
        val xRight = nodeX[rodIdx + 1]
        val rodWidth = max(1.0, xRight - xLeft)
        val rodHeight = rodHeights[rodIdx]
        val rectTop = centerY - rodHeight / 2.0
        val isOverloaded = rodIdx in overloadedRodIndices

        val fillColor = if (isOverloaded) SvgPalette.ROD_OVERLOAD_FILL else SvgPalette.ROD_FILL
        val borderColor = if (isOverloaded) SvgPalette.ROD_OVERLOAD_BORDER else SvgPalette.ROD_BORDER
        val strokeWidth = if (isOverloaded) SvgDefaults.ROD_OVERLOAD_STROKE_WIDTH else SvgDefaults.ROD_STROKE_WIDTH

        sb.appendLine(
            "    <rect x=\"${fmt(xLeft)}\" y=\"${fmt(rectTop)}\" " +
                "width=\"${fmt(rodWidth)}\" height=\"${fmt(rodHeight)}\" " +
                "fill=\"${SvgPalette.CANVAS_BASE}\"/>"
        )
        sb.appendLine(
            "    <rect x=\"${fmt(xLeft)}\" y=\"${fmt(rectTop)}\" " +
                "width=\"${fmt(rodWidth)}\" height=\"${fmt(rodHeight)}\" " +
                "fill=\"$fillColor\" stroke=\"$borderColor\" stroke-width=\"${fmt(strokeWidth)}\"/>"
        )

        if (abs(rod.distributedLoad) > SvgDefaults.FLOAT_EPSILON) {
            appendDistributedLoadSvg(
                sb = sb,
                load = rod.distributedLoad,
                xLeft = xLeft,
                xRight = xRight,
                centerY = centerY,
                rectTop = rectTop,
                rodHeight = rodHeight,
            )
        }

        val centerX = (xLeft + xRight) / 2.0
        if (rodWidth >= SvgDefaults.ROD_BADGE_RADIUS * 2.2) {
            sb.appendLine(
                "    <circle cx=\"${fmt(centerX)}\" cy=\"${fmt(centerY)}\" " +
                    "r=\"${fmt(SvgDefaults.ROD_BADGE_RADIUS)}\" fill=\"${SvgPalette.CANVAS_BASE}\"/>"
            )
            sb.appendLine(
                "    <circle cx=\"${fmt(centerX)}\" cy=\"${fmt(centerY)}\" " +
                    "r=\"${fmt(SvgDefaults.ROD_BADGE_RADIUS)}\" fill=\"${SvgPalette.NODE_FILL}\" " +
                    "stroke=\"${SvgPalette.NODE_BORDER}\" stroke-width=\"${fmt(SvgDefaults.NODE_STROKE_WIDTH)}\"/>"
            )
            sb.appendLine(
                "    <text x=\"${fmt(centerX)}\" y=\"${fmt(centerY)}\" " +
                    "text-anchor=\"middle\" dominant-baseline=\"central\" " +
                    "fill=\"${SvgPalette.ON_NODE}\" class=\"svg-mono\">${rodIdx + 1}</text>"
            )
        }

        // Dimension line and notches
        sb.appendLine(
            "    <line x1=\"${fmt(xLeft)}\" y1=\"${fmt(commonDimY)}\" " +
                "x2=\"${fmt(xRight)}\" y2=\"${fmt(commonDimY)}\" " +
                "stroke=\"${SvgPalette.DIMENSION}\" stroke-width=\"${fmt(SvgDefaults.LINE_STROKE_WIDTH)}\"/>"
        )
        sb.appendLine(
            "    <line x1=\"${fmt(xLeft)}\" y1=\"${fmt(commonDimY - SvgDefaults.DIMENSION_NOTCH_HALF_LEN)}\" " +
                "x2=\"${fmt(xLeft)}\" y2=\"${fmt(commonDimY + SvgDefaults.DIMENSION_NOTCH_HALF_LEN)}\" " +
                "stroke=\"${SvgPalette.DIMENSION}\" stroke-width=\"${fmt(SvgDefaults.LINE_STROKE_WIDTH)}\"/>"
        )
        sb.appendLine(
            "    <line x1=\"${fmt(xRight)}\" y1=\"${fmt(commonDimY - SvgDefaults.DIMENSION_NOTCH_HALF_LEN)}\" " +
                "x2=\"${fmt(xRight)}\" y2=\"${fmt(commonDimY + SvgDefaults.DIMENSION_NOTCH_HALF_LEN)}\" " +
                "stroke=\"${SvgPalette.DIMENSION}\" stroke-width=\"${fmt(SvgDefaults.LINE_STROKE_WIDTH)}\"/>"
        )
        val lengthLabel = "${formatReportNumber(rod.length)}L"
        sb.appendLine(
            "    <text x=\"${fmt(centerX)}\" y=\"${fmt(commonDimY - SvgDefaults.DIMENSION_TEXT_GAP_Y)}\" " +
                "text-anchor=\"middle\" fill=\"${SvgPalette.DIMENSION}\" class=\"svg-mono\">$lengthLabel</text>"
        )
    }
    sb.appendLine("  </g>")

    // Dimension extension lines
    sb.appendLine("  <g class=\"dimension-extensions\">")
    for (nodeIdx in 0 until totalNodes) {
        val x = nodeX[nodeIdx]
        val rodBottom = centerY + maxAdjacentRodHeight(nodeIdx) / 2.0
        if (commonDimY > rodBottom) {
            sb.appendLine(
                "    <line x1=\"${fmt(x)}\" y1=\"${fmt(rodBottom)}\" " +
                    "x2=\"${fmt(x)}\" y2=\"${fmt(commonDimY)}\" " +
                    "stroke=\"${SvgPalette.DIMENSION}\" stroke-width=\"${fmt(SvgDefaults.LINE_STROKE_WIDTH)}\" " +
                    "stroke-dasharray=\"4 3\"/>"
            )
        }
    }
    sb.appendLine("  </g>")

    // Fixed supports
    sb.appendLine("  <g class=\"supports\">")
    for (fixedNodeIdx in construction.fixedNodes.sorted()) {
        if (fixedNodeIdx !in 0 until totalNodes) continue
        appendFixedSupportSvg(
            sb = sb,
            nodeX = nodeX[fixedNodeIdx],
            centerY = centerY,
            isLeftEdge = fixedNodeIdx == 0,
            isRightEdge = fixedNodeIdx == totalNodes - 1,
            adjacentRodHeight = maxAdjacentRodHeight(fixedNodeIdx),
        )
    }
    sb.appendLine("  </g>")

    // Nodal forces
    sb.appendLine("  <g class=\"nodal-forces\">")
    for (nodeIdx in construction.nodeForces.indices) {
        if (nodeIdx !in 0 until totalNodes) continue
        val force = construction.nodeForces[nodeIdx]
        if (abs(force) > SvgDefaults.FLOAT_EPSILON) {
            appendNodalForceSvg(
                sb = sb,
                force = force,
                nodeX = nodeX[nodeIdx],
                centerY = centerY,
            )
        }
    }
    sb.appendLine("  </g>")

    // Nodes
    sb.appendLine("  <g class=\"nodes\">")
    val halfNode = SvgDefaults.NODE_SIZE / 2.0
    for (nodeIdx in 0 until totalNodes) {
        val x = nodeX[nodeIdx]
        val left = x - halfNode
        val top = centerY - halfNode
        sb.appendLine(
            "    <rect x=\"${fmt(left)}\" y=\"${fmt(top)}\" " +
                "width=\"${fmt(SvgDefaults.NODE_SIZE)}\" height=\"${fmt(SvgDefaults.NODE_SIZE)}\" " +
                "rx=\"${fmt(SvgDefaults.NODE_CORNER_RADIUS)}\" fill=\"${SvgPalette.CANVAS_BASE}\"/>"
        )
        sb.appendLine(
            "    <rect x=\"${fmt(left)}\" y=\"${fmt(top)}\" " +
                "width=\"${fmt(SvgDefaults.NODE_SIZE)}\" height=\"${fmt(SvgDefaults.NODE_SIZE)}\" " +
                "rx=\"${fmt(SvgDefaults.NODE_CORNER_RADIUS)}\" fill=\"${SvgPalette.NODE_FILL}\" " +
                "stroke=\"${SvgPalette.NODE_BORDER}\" stroke-width=\"${fmt(SvgDefaults.NODE_STROKE_WIDTH)}\"/>"
        )
        sb.appendLine(
            "    <text x=\"${fmt(x)}\" y=\"${fmt(centerY)}\" " +
                "text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "fill=\"${SvgPalette.ON_NODE}\" class=\"svg-mono\">${nodeIdx + 1}</text>"
        )
    }
    sb.appendLine("  </g>")

    // Vertical dashed guide lines across all epures
    sb.appendLine("  <g class=\"epure-guides\">")
    for (nodeIdx in 0 until totalNodes) {
        val x = nodeX[nodeIdx]
        sb.appendLine(
            "    <line x1=\"${fmt(x)}\" y1=\"${fmt(guideTopY)}\" " +
                "x2=\"${fmt(x)}\" y2=\"${fmt(guideBottomY)}\" " +
                "stroke=\"${SvgPalette.EPURE_GUIDE}\" stroke-width=\"${fmt(SvgDefaults.EPURE_GUIDE_STROKE)}\" " +
                "stroke-dasharray=\"4 4\"/>"
        )
    }
    sb.appendLine("  </g>")

    // Three epure bands: Nx, σx, Ux
    components.forEachIndexed { bandIndex, component ->
        val axisY = firstAxisY + bandIndex * SvgDefaults.EPURE_BAND_SPACING
        appendSingleEpureSvg(
            sb = sb,
            construction = construction,
            displacements = displacements,
            component = component,
            axisY = axisY,
            nodeX = nodeX,
            leftEdgeX = leftEdgeX,
            rightEdgeX = rightEdgeX,
        )
    }

    sb.appendLine("</svg>")
    return sb.toString()
}

private fun appendDistributedLoadSvg(
    sb: StringBuilder,
    load: Double,
    xLeft: Double,
    xRight: Double,
    centerY: Double,
    rectTop: Double,
    rodHeight: Double,
) {
    val direction = if (load > 0.0) 1.0 else -1.0
    val rodWidth = max(1.0, xRight - xLeft)
    val padX = min(SvgDefaults.Q_PAD_X, rodWidth * 0.2)
    val availableWidth = rodWidth - 2.0 * padX

    if (availableWidth >= 3.0) {
        val count = ((availableWidth + SvgDefaults.Q_ARROW_GAP) /
            (SvgDefaults.Q_ARROW_LEN + SvgDefaults.Q_ARROW_GAP))
            .toInt()
            .coerceIn(1, SvgDefaults.MAX_Q_ARROWS_PER_ROW)
        val step = availableWidth / count
        val arrowLen = min(SvgDefaults.Q_ARROW_LEN, step * 0.8)
        val tipLen = min(SvgDefaults.Q_TIP_LEN, arrowLen * 0.5)
        val tipWing = min(SvgDefaults.Q_TIP_WING, rodHeight * 0.35)

        for (i in 0 until count) {
            val intervalCenter = (xLeft + padX) + (i + 0.5) * step
            val arrowStartX = intervalCenter - direction * (arrowLen / 2.0)
            val arrowEndX = intervalCenter + direction * (arrowLen / 2.0)
            val shaftEndX = arrowEndX - direction * (tipLen * 0.4)
            val tipBaseX = arrowEndX - direction * tipLen

            sb.appendLine(
                "    <line x1=\"${fmt(arrowStartX)}\" y1=\"${fmt(centerY)}\" " +
                    "x2=\"${fmt(shaftEndX)}\" y2=\"${fmt(centerY)}\" " +
                    "stroke=\"${SvgPalette.FORCE_AND_LOAD}\" stroke-width=\"${fmt(SvgDefaults.Q_SHAFT_STROKE)}\"/>"
            )
            sb.appendLine(
                "    <polygon points=\"${fmt(arrowEndX)},${fmt(centerY)} " +
                    "${fmt(tipBaseX)},${fmt(centerY - tipWing)} " +
                    "${fmt(tipBaseX)},${fmt(centerY + tipWing)}\" " +
                    "fill=\"${SvgPalette.FORCE_AND_LOAD}\"/>"
            )
        }
    }

    val label = "${formatReportNumber(abs(load))}q"
    sb.appendLine(
        "    <text x=\"${fmt((xLeft + xRight) / 2.0)}\" y=\"${fmt(rectTop - SvgDefaults.Q_TEXT_GAP_Y)}\" " +
            "text-anchor=\"middle\" fill=\"${SvgPalette.FORCE_AND_LOAD}\" class=\"svg-mono\">$label</text>"
    )
}

private fun appendFixedSupportSvg(
    sb: StringBuilder,
    nodeX: Double,
    centerY: Double,
    isLeftEdge: Boolean,
    isRightEdge: Boolean,
    adjacentRodHeight: Double,
) {
    val barHeight = max(SvgDefaults.SUPPORT_MIN_HEIGHT, adjacentRodHeight + SvgDefaults.SUPPORT_EXTRA_MARGIN)
    val topY = centerY - barHeight / 2.0
    val bottomY = centerY + barHeight / 2.0

    sb.appendLine(
        "    <line x1=\"${fmt(nodeX)}\" y1=\"${fmt(topY)}\" " +
            "x2=\"${fmt(nodeX)}\" y2=\"${fmt(bottomY)}\" " +
            "stroke=\"${SvgPalette.SUPPORT_HATCH}\" stroke-width=\"${fmt(SvgDefaults.SUPPORT_WALL_STROKE)}\"/>"
    )

    val directions = when {
        isLeftEdge -> listOf(-1.0)
        isRightEdge -> listOf(1.0)
        else -> listOf(-1.0, 1.0)
    }

    for (dir in directions) {
        var currentY = topY + SvgDefaults.SUPPORT_HATCH_INSET
        val endY = bottomY - SvgDefaults.SUPPORT_HATCH_INSET
        while (currentY <= endY + 1e-6) {
            sb.appendLine(
                "    <line x1=\"${fmt(nodeX)}\" y1=\"${fmt(currentY)}\" " +
                    "x2=\"${fmt(nodeX + dir * SvgDefaults.SUPPORT_HATCH_LEN)}\" " +
                    "y2=\"${fmt(currentY + dir * SvgDefaults.SUPPORT_HATCH_LEN)}\" " +
                    "stroke=\"${SvgPalette.SUPPORT_HATCH}\" stroke-width=\"${fmt(SvgDefaults.SUPPORT_HATCH_STROKE)}\"/>"
            )
            currentY += SvgDefaults.SUPPORT_HATCH_STEP
        }
    }
}

private fun appendNodalForceSvg(
    sb: StringBuilder,
    force: Double,
    nodeX: Double,
    centerY: Double,
) {
    val direction = if (force > 0.0) 1.0 else -1.0
    val startX = nodeX + direction * (SvgDefaults.NODE_SIZE / 2.0)
    val endX = startX + direction * SvgDefaults.NODAL_FORCE_ARROW_LEN
    val shaftEndX = endX - direction * (SvgDefaults.NODAL_FORCE_TIP_LEN * 0.5)
    val tipBaseX = endX - direction * SvgDefaults.NODAL_FORCE_TIP_LEN

    sb.appendLine(
        "    <line x1=\"${fmt(startX)}\" y1=\"${fmt(centerY)}\" " +
            "x2=\"${fmt(shaftEndX)}\" y2=\"${fmt(centerY)}\" " +
            "stroke=\"${SvgPalette.FORCE_AND_LOAD}\" stroke-width=\"${fmt(SvgDefaults.NODAL_FORCE_SHAFT_STROKE)}\"/>"
    )
    sb.appendLine(
        "    <polygon points=\"${fmt(endX)},${fmt(centerY)} " +
            "${fmt(tipBaseX)},${fmt(centerY - SvgDefaults.NODAL_FORCE_TIP_HALF_HEIGHT)} " +
            "${fmt(tipBaseX)},${fmt(centerY + SvgDefaults.NODAL_FORCE_TIP_HALF_HEIGHT)}\" " +
            "fill=\"${SvgPalette.FORCE_AND_LOAD}\"/>"
    )

    val label = "${formatReportNumber(abs(force))}F"
    val anchor = if (direction > 0.0) "start" else "end"
    sb.appendLine(
        "    <text x=\"${fmt(startX)}\" y=\"${fmt(centerY - SvgDefaults.NODAL_FORCE_TEXT_GAP_Y)}\" " +
            "text-anchor=\"$anchor\" fill=\"${SvgPalette.FORCE_AND_LOAD}\" class=\"svg-mono\">$label</text>"
    )
}

private fun appendSingleEpureSvg(
    sb: StringBuilder,
    construction: Construction,
    displacements: DoubleArray,
    component: SvgEpureComponent,
    axisY: Double,
    nodeX: DoubleArray,
    leftEdgeX: Double,
    rightEdgeX: Double,
) {
    val rodSamples = construction.rods.indices.map { rodIndex ->
        val rod = construction.rods[rodIndex]
        val segments = if (component == SvgEpureComponent.Displacement && abs(rod.distributedLoad) > SvgDefaults.ZERO_EPS) {
            SvgDefaults.EPURE_PARABOLA_SEGMENTS
        } else {
            1
        }
        List(segments + 1) { step ->
            val fraction = step.toDouble() / segments.toDouble()
            val localX = rod.length * fraction
            val section = calculateSection(construction, displacements, rodIndex, localX)
            fraction to component.extractValue(section)
        }
    }

    var maxAbsValue = 0.0
    for (samples in rodSamples) {
        for ((_, value) in samples) {
            maxAbsValue = max(maxAbsValue, abs(value))
        }
    }

    val epureScale = if (maxAbsValue > SvgDefaults.ZERO_EPS) {
        SvgDefaults.EPURE_HALF_HEIGHT / maxAbsValue
    } else {
        0.0
    }

    sb.appendLine("  <g class=\"epure-${component.name.lowercase()}\">")

    // Component title on the left of the zero axis
    sb.appendLine(
        "    <text x=\"${fmt(leftEdgeX - 14.0)}\" y=\"${fmt(axisY)}\" " +
            "text-anchor=\"end\" dominant-baseline=\"central\" " +
            "fill=\"${SvgPalette.EPURE_LABEL}\" class=\"svg-mono svg-epure-title\">${component.symbol}</text>"
    )

    // Zero axis line
    sb.appendLine(
        "    <line x1=\"${fmt(leftEdgeX)}\" y1=\"${fmt(axisY)}\" " +
            "x2=\"${fmt(rightEdgeX)}\" y2=\"${fmt(axisY)}\" " +
            "stroke=\"${SvgPalette.EPURE_AXIS}\" stroke-width=\"${fmt(SvgDefaults.EPURE_AXIS_STROKE)}\"/>"
    )

    for (rodIndex in construction.rods.indices) {
        val rod = construction.rods[rodIndex]
        val samples = rodSamples[rodIndex]
        val xLeft = nodeX[rodIndex]
        val xRight = nodeX[rodIndex + 1]
        val rodWidth = xRight - xLeft
        if (rodWidth <= 0.5) continue

        val screenPoints = samples.map { (fraction, value) ->
            val sx = xLeft + fraction * rodWidth
            val sy = axisY - value * epureScale
            Triple(sx, sy, value)
        }

        val pathData = buildString {
            append("M ${fmt(xLeft)} ${fmt(axisY)}")
            for ((sx, sy, _) in screenPoints) {
                append(" L ${fmt(sx)} ${fmt(sy)}")
            }
            append(" L ${fmt(xRight)} ${fmt(axisY)}")
        }

        if (epureScale > 0.0) {
            sb.appendLine("    <path d=\"$pathData Z\" fill=\"${SvgPalette.EPURE_FILL}\" stroke=\"none\"/>")

            // Vertical hatch lines matching EpureRenderer
            val firstHatchIndex = ceil((xLeft - leftEdgeX) / SvgDefaults.EPURE_HATCH_STEP).toInt()
            var hatchIndex = firstHatchIndex
            val hatchPath = StringBuilder()
            while (true) {
                val hatchX = leftEdgeX + hatchIndex * SvgDefaults.EPURE_HATCH_STEP
                if (hatchX >= xRight - 0.5) break
                if (hatchX > xLeft + 0.5) {
                    val fraction = ((hatchX - xLeft) / rodWidth).coerceIn(0.0, 1.0)
                    val localX = rod.length * fraction
                    val value = component.extractValue(
                        calculateSection(construction, displacements, rodIndex, localX)
                    )
                    if (abs(value) > SvgDefaults.ZERO_EPS) {
                        val yVal = axisY - value * epureScale
                        if (hatchPath.isNotEmpty()) hatchPath.append(' ')
                        hatchPath.append("M ${fmt(hatchX)} ${fmt(axisY)} L ${fmt(hatchX)} ${fmt(yVal)}")
                    }
                }
                hatchIndex++
            }
            if (hatchPath.isNotEmpty()) {
                sb.appendLine(
                    "    <path d=\"$hatchPath\" fill=\"none\" " +
                        "stroke=\"${SvgPalette.EPURE_HATCH}\" stroke-width=\"${fmt(SvgDefaults.EPURE_HATCH_STROKE)}\"/>"
                )
            }
        }

        // Contour
        sb.appendLine(
            "    <path d=\"$pathData\" fill=\"none\" " +
                "stroke=\"${SvgPalette.EPURE_STROKE}\" stroke-width=\"${fmt(SvgDefaults.EPURE_CONTOUR_STROKE)}\" " +
                "stroke-linejoin=\"round\"/>"
        )

        // Value labels
        val startValue = samples.first().second
        val endValue = samples.last().second
        val startY = screenPoints.first().second
        val endY = screenPoints.last().second
        val isConstant = abs(startValue - endValue) <= SvgDefaults.ZERO_EPS &&
            !(component == SvgEpureComponent.Displacement && abs(rod.distributedLoad) > SvgDefaults.ZERO_EPS)

        if (isConstant) {
            appendEpureValueLabelSvg(
                sb = sb,
                value = startValue,
                anchorX = (xLeft + xRight) / 2.0,
                anchorY = startY,
                axisY = axisY,
                align = SvgLabelAlign.Center,
                screenPoints = screenPoints,
            )
        } else {
            appendEpureValueLabelSvg(
                sb = sb,
                value = startValue,
                anchorX = xLeft + SvgDefaults.EPURE_LABEL_INSET_X,
                anchorY = startY,
                axisY = axisY,
                align = SvgLabelAlign.Left,
                screenPoints = screenPoints,
            )
            appendEpureValueLabelSvg(
                sb = sb,
                value = endValue,
                anchorX = xRight - SvgDefaults.EPURE_LABEL_INSET_X,
                anchorY = endY,
                axisY = axisY,
                align = SvgLabelAlign.Right,
                screenPoints = screenPoints,
            )
        }
    }

    sb.appendLine("  </g>")
}

private fun appendEpureValueLabelSvg(
    sb: StringBuilder,
    value: Double,
    anchorX: Double,
    anchorY: Double,
    axisY: Double,
    align: SvgLabelAlign,
    screenPoints: List<Triple<Double, Double, Double>>,
) {
    val labelText = formatReportNumber(abs(value))
    val estimatedWidth = labelText.length * SvgDefaults.CHAR_WIDTH_ESTIMATE
    val spanLeft = when (align) {
        SvgLabelAlign.Left -> anchorX
        SvgLabelAlign.Center -> anchorX - estimatedWidth / 2.0
        SvgLabelAlign.Right -> anchorX - estimatedWidth
    }
    val (minCurveY, maxCurveY) = svgCurveYBoundsInRange(
        screenPoints = screenPoints,
        spanLeft = spanLeft - SvgDefaults.EPURE_LABEL_CURVE_CLEARANCE_X,
        spanRight = spanLeft + estimatedWidth + SvgDefaults.EPURE_LABEL_CURVE_CLEARANCE_X,
        fallbackY = anchorY,
    )
    val textAnchor = when (align) {
        SvgLabelAlign.Left -> "start"
        SvgLabelAlign.Center -> "middle"
        SvgLabelAlign.Right -> "end"
    }
    val textBaselineY = if (anchorY <= axisY) {
        min(anchorY, minCurveY) - SvgDefaults.EPURE_LABEL_GAP_Y
    } else {
        max(anchorY, maxCurveY) + SvgDefaults.EPURE_LABEL_GAP_Y + SvgDefaults.TEXT_HEIGHT_ESTIMATE * 0.85
    }

    sb.appendLine(
        "    <text x=\"${fmt(anchorX)}\" y=\"${fmt(textBaselineY)}\" " +
            "text-anchor=\"$textAnchor\" fill=\"${SvgPalette.EPURE_LABEL}\" class=\"svg-mono\">$labelText</text>"
    )
}

private fun svgCurveYBoundsInRange(
    screenPoints: List<Triple<Double, Double, Double>>,
    spanLeft: Double,
    spanRight: Double,
    fallbackY: Double,
): Pair<Double, Double> {
    var minY = fallbackY
    var maxY = fallbackY

    for (i in 0 until screenPoints.lastIndex) {
        val (x0, y0, _) = screenPoints[i]
        val (x1, y1, _) = screenPoints[i + 1]
        val segMinX = min(x0, x1)
        val segMaxX = max(x0, x1)
        if (segMaxX < spanLeft || segMinX > spanRight) continue

        if (x0 in spanLeft..spanRight) {
            minY = min(minY, y0)
            maxY = max(maxY, y0)
        }
        if (x1 in spanLeft..spanRight) {
            minY = min(minY, y1)
            maxY = max(maxY, y1)
        }

        val dx = x1 - x0
        if (abs(dx) > 1e-4) {
            if (spanLeft in segMinX..segMaxX) {
                val t = ((spanLeft - x0) / dx).coerceIn(0.0, 1.0)
                val y = y0 + t * (y1 - y0)
                minY = min(minY, y)
                maxY = max(maxY, y)
            }
            if (spanRight in segMinX..segMaxX) {
                val t = ((spanRight - x0) / dx).coerceIn(0.0, 1.0)
                val y = y0 + t * (y1 - y0)
                minY = min(minY, y)
                maxY = max(maxY, y)
            }
        }
    }

    return minY to maxY
}

private fun svgDisplayRodLength(rod: Rod): Double =
    rod.length.coerceAtMost(SvgDefaults.MAX_WORLD_ROD_LENGTH)

private fun svgBaseRodHeight(rod: Rod): Double =
    (rod.crossSectionArea * SvgDefaults.BASE_ROD_HEIGHT_PER_AREA)
        .coerceIn(SvgDefaults.MIN_BASE_ROD_HEIGHT, SvgDefaults.MAX_BASE_ROD_HEIGHT)

private fun fmt(value: Double): String {
    if (!value.isFinite()) return "0"
    val scaled = floor(abs(value) * 100.0 + 0.5 + 1e-9).toLong()
    if (scaled == 0L) return "0"
    val sign = if (value < 0.0) "-" else ""
    val integerPart = scaled / 100L
    val fractionalPart = (scaled % 100L).toString().padStart(2, '0').trimEnd('0')
    return if (fractionalPart.isEmpty()) {
        "$sign$integerPart"
    } else {
        "$sign$integerPart.$fractionalPart"
    }
}
