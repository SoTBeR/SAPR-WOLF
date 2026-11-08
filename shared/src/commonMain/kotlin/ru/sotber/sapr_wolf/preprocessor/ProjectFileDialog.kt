package ru.sotber.sapr_wolf.preprocessor

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString

object ProjectFileDialog {
    private const val JSON_EXTENSION = "json"
    private const val DEFAULT_NAME = "project"

    suspend fun pickJsonFileToOpen(): Result<String>? {
        val selectedFile = runCatching {
            FileKit.openFilePicker(
                type = FileKitType.File(extensions = listOf(JSON_EXTENSION)),
            )
        }.getOrElse { error ->
            return Result.failure(error)
        } ?: return null

        return runCatching {
            selectedFile.readString()
        }
    }

    suspend fun saveJsonFile(
        defaultFileName: String,
        content: String,
    ): Result<Unit>? {
        val suggestedBaseName = defaultFileName
            .removeSuffix(".json")
            .removeSuffix(".JSON")
            .ifBlank { DEFAULT_NAME }

        val targetFile = runCatching {
            FileKit.openFileSaver(
                suggestedName = suggestedBaseName,
                defaultExtension = JSON_EXTENSION,
            )
        }.getOrElse { error ->
            return Result.failure(error)
        } ?: return null

        return runCatching {
            targetFile.writeString(content)
        }
    }
}
