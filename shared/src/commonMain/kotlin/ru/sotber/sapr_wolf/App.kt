package ru.sotber.sapr_wolf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import ru.sotber.sapr_wolf.ui.MainScreen
import ru.sotber.sapr_wolf.ui.components.splash.StartupSplashOverlay
import ru.sotber.sapr_wolf.ui.theme.AppTheme
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors

@Composable
fun App(
    appViewModel: AppViewModel = viewModel { AppViewModel() },
) {
    val currentTheme by appViewModel.appTheme.collectAsStateWithLifecycle()
    val isSplashVisible by appViewModel.isSplashVisible.collectAsStateWithLifecycle()

    AppTheme(appTheme = currentTheme) {
        val isDarkTheme = MaterialThemeExtraColors.isDark
        val splashHazeState = rememberHazeState()

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(splashHazeState),
            ) {
                MainScreen(
                    onToggleTheme = { appViewModel.toggleAppTheme(isDarkTheme) },
                )
            }

            StartupSplashOverlay(
                visible = isSplashVisible,
                hazeState = splashHazeState,
                onDismiss = appViewModel::dismissSplash,
            )
        }
    }
}
