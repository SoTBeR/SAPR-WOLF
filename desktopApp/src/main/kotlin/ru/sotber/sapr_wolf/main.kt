package ru.sotber.sapr_wolf

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.ic_app_logo
import sapr_wolf.shared.generated.resources.app_name

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.ic_app_logo),
    ) {
        App()
    }
}
