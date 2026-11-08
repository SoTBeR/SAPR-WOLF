package ru.sotber.sapr_wolf.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ViewportState(
    initialZoom: Float = BASE_ZOOM,
    initialHeightScale: Float = 1.0f,
    initialPanX: Float = 0f,
    initialPanY: Float = 0f,
) {
    var zoomX by mutableStateOf(initialZoom)
    var heightScale by mutableStateOf(initialHeightScale)
    var panX by mutableStateOf(initialPanX)
    var panY by mutableStateOf(initialPanY)

    var isInitialized by mutableStateOf(false)
    val scaleFactor: Float get() = heightScale.coerceIn(MIN_SCALE, MAX_SCALE)
    val zoomPercent: Double get() = (zoomX / BASE_ZOOM) * 100.0

    fun worldToScreenX(worldX: Double, canvasWidth: Float, totalConstructionLength: Double): Float {
        val worldCenterX = totalConstructionLength / 2.0
        val screenCenterX = canvasWidth / 2f + panX
        return (screenCenterX + zoomX * (worldX - worldCenterX)).toFloat()
    }

    fun screenToWorldX(screenX: Float, canvasWidth: Float, totalConstructionLength: Double): Double {
        val worldCenterX = totalConstructionLength / 2.0
        val screenCenterX = canvasWidth / 2f + panX
        return worldCenterX + ((screenX - screenCenterX) / zoomX).toDouble()
    }

    fun zoomAt(
        cursorScreenX: Float,
        cursorScreenY: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        zoomFactorChange: Float,
        horizontalOnly: Boolean,
    ) {
        if (horizontalOnly) {
            val oldZoomX = zoomX
            val newZoomX = (oldZoomX * zoomFactorChange).coerceIn(MIN_ZOOM_X, MAX_ZOOM_X)
            val appliedFactorX = newZoomX / oldZoomX
            if (appliedFactorX == 1f) return

            val cursorRelX = cursorScreenX - canvasWidth / 2f
            panX = cursorRelX - (cursorRelX - panX) * appliedFactorX
            zoomX = newZoomX
            return
        }

        val minAllowedFactor = maxOf(MIN_ZOOM_X / zoomX, MIN_SCALE / heightScale).coerceAtMost(1f)
        val maxAllowedFactor = minOf(MAX_ZOOM_X / zoomX, MAX_SCALE / heightScale).coerceAtLeast(1f)
        val appliedFactor = zoomFactorChange.coerceIn(minAllowedFactor, maxAllowedFactor)
        if (appliedFactor == 1f) return

        val cursorRelX = cursorScreenX - canvasWidth / 2f
        val cursorRelY = cursorScreenY - canvasHeight / 2f

        panX = cursorRelX - (cursorRelX - panX) * appliedFactor
        panY = cursorRelY - (cursorRelY - panY) * appliedFactor
        zoomX = (zoomX * appliedFactor).coerceIn(MIN_ZOOM_X, MAX_ZOOM_X)
        heightScale = (heightScale * appliedFactor).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun pan(deltaX: Float, deltaY: Float) {
        panX += deltaX
        panY += deltaY
    }

    fun fitToView(canvasWidth: Float, totalConstructionLength: Double) {
        if (canvasWidth <= 0f) return
        if (totalConstructionLength <= 0.0) {
            resetTo1to1()
            isInitialized = true
            return
        }

        val availableWidth = (canvasWidth - FIT_HORIZONTAL_PADDING * 2f).coerceAtLeast(MIN_AVAILABLE_WIDTH)
        val effectiveWorldWidth = totalConstructionLength + FIT_SIDE_WORLD_ALLOWANCE
        val fitScale = ((availableWidth / effectiveWorldWidth).toFloat() / BASE_ZOOM)
            .coerceIn(MIN_FIT_SCALE, MAX_FIT_SCALE)

        zoomX = fitScale * BASE_ZOOM
        heightScale = fitScale
        panX = 0f
        panY = 0f
        isInitialized = true
    }

    fun resetTo1to1(horizontalOnly: Boolean = false) {
        zoomX = BASE_ZOOM
        panX = 0f
        if (!horizontalOnly) {
            heightScale = 1.0f
            panY = 0f
        }
    }

    companion object {
        const val BASE_ZOOM = 80f
        const val MIN_SCALE = 0.25f
        const val MAX_SCALE = 9.99f
        const val MIN_ZOOM_X = BASE_ZOOM * MIN_SCALE
        const val MAX_ZOOM_X = BASE_ZOOM * MAX_SCALE

        private const val MIN_FIT_SCALE = 0.25f
        private const val MAX_FIT_SCALE = 2.5f
        private const val FIT_HORIZONTAL_PADDING = 64f
        private const val MIN_AVAILABLE_WIDTH = 120f
        private const val FIT_SIDE_WORLD_ALLOWANCE = 2.0
    }
}
