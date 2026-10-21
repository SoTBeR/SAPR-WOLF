package ru.sotber.sapr_wolf

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SAPR_WOLF",
    ) {
        App()
    }
}