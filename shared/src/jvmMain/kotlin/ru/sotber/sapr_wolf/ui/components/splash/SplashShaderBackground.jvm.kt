package ru.sotber.sapr_wolf.ui.components.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

@Composable
actual fun SplashShaderBackground(
    isDarkTheme: Boolean,
    modifier: Modifier,
) {
    val timeSeconds = rememberSplashShaderTime()
    val runtimeEffect = remember {
        runCatching { RuntimeEffect.makeForShader(LIQUID_METAL_SHADER) }.getOrNull()
    }

    if (runtimeEffect == null) {
        SplashShaderFallback(
            isDarkTheme = isDarkTheme,
            timeSeconds = timeSeconds,
            modifier = modifier,
        )
        return
    }

    val shaderBuilder = remember(runtimeEffect) { RuntimeShaderBuilder(runtimeEffect) }
    val skiaPaint = remember { Paint().apply { isAntiAlias = true } }

    DisposableEffect(runtimeEffect, skiaPaint) {
        onDispose {
            skiaPaint.close()
            runtimeEffect.close()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        shaderBuilder.uniform("iResolution", size.width, size.height)
        shaderBuilder.uniform("iTime", timeSeconds)
        shaderBuilder.uniform("iDarkTheme", if (isDarkTheme) 1f else 0f)
        val shader = shaderBuilder.makeShader()
        try {
            skiaPaint.shader = shader
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(
                    Rect.makeWH(size.width, size.height),
                    skiaPaint,
                )
            }
        } finally {
            skiaPaint.shader = null
            shader.close()
        }
    }
}
