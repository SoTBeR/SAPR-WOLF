package ru.sotber.sapr_wolf

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.sotber.sapr_wolf.ui.theme.AppTheme

class AppViewModel : ViewModel() {
    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
    }

    fun toggleAppTheme(currentlyDark: Boolean) {
        _appTheme.value = if (currentlyDark) AppTheme.LIGHT else AppTheme.DARK
    }
}
