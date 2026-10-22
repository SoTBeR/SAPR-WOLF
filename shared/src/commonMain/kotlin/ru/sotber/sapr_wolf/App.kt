package ru.sotber.sapr_wolf

import androidx.compose.runtime.Composable
import ru.sotber.sapr_wolf.ui.MainScreen
import ru.sotber.sapr_wolf.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme(appTheme = AppTheme.DARK) {
        MainScreen()
    }
}
