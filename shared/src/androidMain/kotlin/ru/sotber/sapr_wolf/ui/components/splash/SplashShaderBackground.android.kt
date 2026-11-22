package ru.sotber.sapr_wolf.ui.components.splash

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush

@Composable
actual fun SplashShaderBackground(
    isDarkTheme: Boolean,
    modifier: Modifier,
) {
    val timeSeconds = rememberSplashShaderTime()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val runtimeShader = remember {
            runCatching { RuntimeShader(LIQUID_METAL_SHADER) }.getOrNull()
        }
        if (runtimeShader != null) {
            val shaderBrush = remember(runtimeShader) { ShaderBrush(runtimeShader) }
            Canvas(modifier = modifier.fillMaxSize()) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                runtimeShader.setFloatUniform("iTime", timeSeconds)
                runtimeShader.setFloatUniform("iDarkTheme", if (isDarkTheme) 1f else 0f)
                drawRect(brush = shaderBrush)
            }
            return
        }
    }

    SplashShaderFallback(
        isDarkTheme = isDarkTheme,
        timeSeconds = timeSeconds,
        modifier = modifier,
    )
}
