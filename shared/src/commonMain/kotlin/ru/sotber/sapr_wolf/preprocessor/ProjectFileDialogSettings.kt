package ru.sotber.sapr_wolf.preprocessor

import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType

internal expect fun createProjectFileDialogSettings(): FileKitDialogSettings

internal expect fun createProjectOpenFileType(): FileKitType
