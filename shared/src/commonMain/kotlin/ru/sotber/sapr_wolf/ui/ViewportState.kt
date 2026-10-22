package ru.sotber.sapr_wolf.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ViewportState(
    initialZoom: Float = 80f,
    initialHeightScale: Float = 1.0f,
    initialPanX: Float = 0f,
    initialPanY: Float = 0f
) {
    var zoomX by mutableStateOf(initialZoom)
    var heightScale by mutableStateOf(initialHeightScale)
    var panX by mutableStateOf(initialPanX)
    var panY by mutableStateOf(initialPanY)

    var isInitialized by mutableStateOf(false)

    val zoom: Float get() = zoomX
    val scaleFactor: Float get() = heightScale.coerceIn(0.2f, 4.0f)

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
        horizontalOnly: Boolean
    ) {
        val oldZoomX = zoomX
        val newZoomX = (oldZoomX * zoomFactorChange).coerceIn(10f, 450f)
        if (newZoomX == oldZoomX) return

        val cursorRelX = cursorScreenX - canvasWidth / 2f
        panX = cursorRelX - (cursorRelX - panX) * (newZoomX / oldZoomX)
        zoomX = newZoomX

        if (!horizontalOnly) {
            val oldHeightScale = heightScale
            val newHeightScale = (oldHeightScale * zoomFactorChange).coerceIn(0.25f, 3.5f)
            heightScale = newHeightScale

            val cursorRelY = cursorScreenY - canvasHeight / 2f
            panY = cursorRelY - (cursorRelY - panY) * (newHeightScale / oldHeightScale)
        }
    }

    fun pan(deltaX: Float, deltaY: Float) {
        panX += deltaX
        panY += deltaY
    }

    fun fitToView(canvasWidth: Float, totalConstructionLength: Double) {
        if (totalConstructionLength <= 0.0 || canvasWidth <= 0f) return
        val padding = 140f
        val availableWidth = (canvasWidth - padding * 2).coerceAtLeast(100f)
        zoomX = (availableWidth / totalConstructionLength).toFloat().coerceIn(20f, 300f)
        heightScale = (zoomX / 80f).coerceIn(0.7f, 1.8f)
        panX = 0f
        panY = 0f
        isInitialized = true
    }

    fun resetTo1to1(horizontalOnly: Boolean = false) {
        zoomX = 80f
        panX = 0f
        if (!horizontalOnly) {
            heightScale = 1.0f
            panY = 0f
        }
    }
}
