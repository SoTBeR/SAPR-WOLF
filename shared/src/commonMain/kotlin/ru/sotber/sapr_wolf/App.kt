package ru.sotber.sapr_wolf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.sotber.sapr_wolf.ui.MainScreen
import ru.sotber.sapr_wolf.ui.theme.AppTheme
import ru.sotber.sapr_wolf.ui.theme.MaterialThemeExtraColors

@Composable
fun App(
    appViewModel: AppViewModel = viewModel { AppViewModel() },
) {
    val currentTheme by appViewModel.appTheme.collectAsStateWithLifecycle()

    AppTheme(appTheme = currentTheme) {
        val isDarkTheme = MaterialThemeExtraColors.isDark

        MainScreen(
            onToggleTheme = { appViewModel.toggleTheme(isDarkTheme) },
        )
    }
}
