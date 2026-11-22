package ru.sotber.sapr_wolf.preprocessor

import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication

class ProjectFileHandle(
    val platformFile: PlatformFile? = null,
)

data class OpenedProjectFile(
    val file: ProjectFileHandle,
    val projectName: String,
    val content: String,
    val isLegacyJson: Boolean = false,
)

data class SavedProjectFile(
    val file: ProjectFileHandle,
    val projectName: String,
)

interface ProjectFileGateway {
    suspend fun pickProjectFileToOpen(): Result<OpenedProjectFile>?

    suspend fun saveProjectFileAs(
        defaultFileName: String,
        content: String,
    ): Result<SavedProjectFile>?

    suspend fun overwriteProjectFile(
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
    const val PROJECT_EXTENSION = "sprw"
    const val LEGACY_JSON_EXTENSION = "json"
    private const val DEFAULT_NAME = "project"

    override suspend fun pickProjectFileToOpen(): Result<OpenedProjectFile>? {
        val selectedFile = runCatching {
            FileKit.openFilePicker(
                type = createProjectOpenFileType(),
                dialogSettings = createProjectFileDialogSettings(),
            )
        }.getOrElse { error ->
            return Result.failure(error)
        } ?: return null

        return runCatching {
            val fileName = selectedFile.name
            OpenedProjectFile(
                file = ProjectFileHandle(selectedFile),
                projectName = extractProjectName(fileName),
                content = selectedFile.readString(),
                isLegacyJson = isLegacyJsonFileName(fileName),
            )
        }
    }

    override suspend fun saveProjectFileAs(
        defaultFileName: String,
        content: String,
    ): Result<SavedProjectFile>? {
        val suggestedBaseName = extractProjectName(defaultFileName)

        val targetFile = runCatching {
            FileKit.openFileSaver(
                suggestedName = suggestedBaseName,
                defaultExtension = PROJECT_EXTENSION,
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

    override suspend fun overwriteProjectFile(
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

    internal fun isLegacyJsonFileName(fileName: String): Boolean =
        fileName.trim().endsWith(".$LEGACY_JSON_EXTENSION", ignoreCase = true)

    internal fun extractProjectName(fileName: String): String {
        val trimmed = fileName.trim()
        val withoutExtension = when {
            trimmed.endsWith(".$PROJECT_EXTENSION", ignoreCase = true) ->
                trimmed.dropLast(PROJECT_EXTENSION.length + 1)
            trimmed.endsWith(".$LEGACY_JSON_EXTENSION", ignoreCase = true) ->
                trimmed.dropLast(LEGACY_JSON_EXTENSION.length + 1)
            else -> trimmed
        }

        return withoutExtension.ifBlank { DEFAULT_NAME }
    }
}
