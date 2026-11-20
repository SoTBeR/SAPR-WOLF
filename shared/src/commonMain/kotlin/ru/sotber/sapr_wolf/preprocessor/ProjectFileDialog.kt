package ru.sotber.sapr_wolf.preprocessor

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString

class ProjectFileHandle(
    val platformFile: PlatformFile? = null,
)

data class OpenedProjectFile(
    val file: ProjectFileHandle,
    val projectName: String,
    val content: String,
)

data class SavedProjectFile(
    val file: ProjectFileHandle,
    val projectName: String,
)

interface ProjectFileGateway {
    suspend fun pickJsonFileToOpen(): Result<OpenedProjectFile>?

    suspend fun saveJsonFileAs(
        defaultFileName: String,
        content: String,
    ): Result<SavedProjectFile>?

    suspend fun overwriteJsonFile(
        file: ProjectFileHandle,
        content: String,
    ): Result<Unit>

    suspend fun saveReportFile(
        defaultFileName: String,
        extension: String,
        content: String,
        openAfterExport: Boolean = false,
    ): Result<String>? = null
}

object ProjectFileDialog : ProjectFileGateway {
    private const val JSON_EXTENSION = "json"
    private const val DEFAULT_NAME = "project"

    override suspend fun pickJsonFileToOpen(): Result<OpenedProjectFile>? {
        val selectedFile = runCatching {
            FileKit.openFilePicker(
                type = FileKitType.File(extensions = listOf(JSON_EXTENSION)),
                dialogSettings = createProjectFileDialogSettings(),
            )
        }.getOrElse { error ->
            return Result.failure(error)
        } ?: return null

        return runCatching {
            OpenedProjectFile(
                file = ProjectFileHandle(selectedFile),
                projectName = extractProjectName(selectedFile.name),
                content = selectedFile.readString(),
            )
        }
    }

    override suspend fun saveJsonFileAs(
        defaultFileName: String,
        content: String,
    ): Result<SavedProjectFile>? {
        val suggestedBaseName = extractProjectName(defaultFileName)

        val targetFile = runCatching {
            FileKit.openFileSaver(
                suggestedName = suggestedBaseName,
                defaultExtension = JSON_EXTENSION,
                dialogSettings = createProjectFileDialogSettings(),
            )
        }.getOrElse { error ->
            return Result.failure(error)
        } ?: return null

        return runCatching {
            targetFile.writeString(content)
            SavedProjectFile(
                file = ProjectFileHandle(targetFile),
                projectName = extractProjectName(targetFile.name),
            )
        }
    }

    override suspend fun overwriteJsonFile(
        file: ProjectFileHandle,
        content: String,
    ): Result<Unit> = runCatching {
        val targetFile = requireNotNull(file.platformFile) {
            "Файл проекта недоступен для перезаписи"
        }
        targetFile.writeString(content)
    }

    override suspend fun saveReportFile(
        defaultFileName: String,
        extension: String,
        content: String,
        openAfterExport: Boolean,
    ): Result<String>? {
        val suggestedBaseName = extractProjectName(defaultFileName) + "_report"

        val targetFile = runCatching {
            FileKit.openFileSaver(
                suggestedName = suggestedBaseName,
                defaultExtension = extension,
                dialogSettings = createProjectFileDialogSettings(),
            )
        }.getOrElse { error ->
            return Result.failure(error)
        } ?: return null

        return runCatching {
            targetFile.writeString(content)
            if (openAfterExport) {
                runCatching {
                    FileKit.openFileWithDefaultApplication(targetFile)
                }
            }
            targetFile.name
        }
    }

    internal fun extractProjectName(fileName: String): String {
        val trimmed = fileName.trim()
        val withoutExtension = if (trimmed.endsWith(".json", ignoreCase = true)) {
            trimmed.dropLast(5)
        } else {
            trimmed
        }

        return withoutExtension.ifBlank { DEFAULT_NAME }
    }
}
