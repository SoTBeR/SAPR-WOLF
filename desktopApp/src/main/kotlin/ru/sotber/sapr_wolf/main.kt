package ru.sotber.sapr_wolf

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.sotber.sapr_wolf.preprocessor.registerDesktopFileDialogParent
import sapr_wolf.shared.generated.resources.Res
import sapr_wolf.shared.generated.resources.app_name
import sapr_wolf.shared.generated.resources.ic_app_logo

fun main() {
    FileKit.init(appId = "ru.sotber.sapr_wolf")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.ic_app_logo),
        ) {
            DisposableEffect(window) {
                registerDesktopFileDialogParent(window)
                onDispose {
                    registerDesktopFileDialogParent(null)
                }
            }
            App()
        }
    }
}
