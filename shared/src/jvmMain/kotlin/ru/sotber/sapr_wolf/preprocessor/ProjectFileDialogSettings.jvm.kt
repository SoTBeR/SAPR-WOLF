package ru.sotber.sapr_wolf.preprocessor

import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import java.awt.Window
import java.lang.ref.WeakReference

private var desktopParentWindowRef: WeakReference<Window>? = null

fun registerDesktopFileDialogParent(window: Window?) {
    desktopParentWindowRef = window?.let(::WeakReference)
}

internal actual fun createProjectFileDialogSettings(): FileKitDialogSettings {
    val ownerWindow = desktopParentWindowRef?.get()?.takeIf { it.isDisplayable }
        ?: Window.getWindows().firstOrNull { it.isDisplayable && it.isActive }
        ?: Window.getWindows().firstOrNull { it.isDisplayable }

    return FileKitDialogSettings(
        parent = ownerWindow?.let(FileKitDialogParent::awt),
    )
}

internal actual fun createProjectOpenFileType(): FileKitType =
    FileKitType.File(
        extensions = listOf(
            ProjectFileDialog.PROJECT_EXTENSION,
            ProjectFileDialog.LEGACY_JSON_EXTENSION,
        ),
    )
