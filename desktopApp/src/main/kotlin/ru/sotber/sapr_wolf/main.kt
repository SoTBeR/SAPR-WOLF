package ru.sotber.sapr_wolf

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.app_logo

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SAPR-WOLF",
        icon = painterResource(Res.drawable.app_logo)
    ) {
        App()
    }
}
