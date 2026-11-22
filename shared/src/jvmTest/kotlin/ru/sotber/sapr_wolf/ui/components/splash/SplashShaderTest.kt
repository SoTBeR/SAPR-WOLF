package ru.sotber.sapr_wolf.ui.components.splash

import ru.sotber.sapr_wolf.AppViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SplashShaderTest {

    @Test
    fun liquidMetalShaderDefinesExpectedUniformsAndEntryPoint() {
        assertTrue(LIQUID_METAL_SHADER.contains("uniform float2 iResolution;"))
        assertTrue(LIQUID_METAL_SHADER.contains("uniform float iTime;"))
        assertTrue(LIQUID_METAL_SHADER.contains("uniform float iDarkTheme;"))
        assertTrue(LIQUID_METAL_SHADER.contains("half4 main(float2 fragCoord)"))
    }

    @Test
    fun appViewModelShowsSplashInitiallyAndDismissesIt() {
        val viewModel = AppViewModel()
        assertTrue(viewModel.isSplashVisible.value)

        viewModel.dismissSplash()
        assertFalse(viewModel.isSplashVisible.value)
    }
}
