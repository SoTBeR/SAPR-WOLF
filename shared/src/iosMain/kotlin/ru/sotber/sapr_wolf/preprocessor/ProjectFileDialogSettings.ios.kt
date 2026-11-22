package ru.sotber.sapr_wolf.preprocessor

import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType

internal actual fun createProjectFileDialogSettings(): FileKitDialogSettings =
    FileKitDialogSettings.createDefault()

internal actual fun createProjectOpenFileType(): FileKitType =
    FileKitType.File(
        extensions = listOf(
            ProjectFileDialog.PROJECT_EXTENSION,
            ProjectFileDialog.LEGACY_JSON_EXTENSION,
        ),
    )
