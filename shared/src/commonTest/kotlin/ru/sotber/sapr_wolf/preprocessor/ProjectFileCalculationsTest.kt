package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.*
import ru.sotber.sapr_wolf.postprocessor.analyzeStrength
import ru.sotber.sapr_wolf.postprocessor.calculateEquilibriumResidual
import ru.sotber.sapr_wolf.postprocessor.calculateReactions
import ru.sotber.sapr_wolf.processor.calculateDisplacements
import ru.sotber.sapr_wolf.ui.CalculationState
import ru.sotber.sapr_wolf.ui.FileDialogAlert
import ru.sotber.sapr_wolf.ui.MainViewModel
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.*

class ProjectFileCalculationsTest {

    private fun buildValidSavedCalculation(construction: Construction): SavedCalculation {
        val displacements = calculateDisplacements(construction).getOrThrow()
        val reactions = calculateReactions(construction, displacements)
        val residual = calculateEquilibriumResidual(construction, reactions)
        val strengths = analyzeStrength(construction, displacements)
        return SavedCalculation(
            displacements = displacements.toList(),
            reactions = reactions,
            equilibriumResidual = residual,
            rodStrengths = strengths,
        )
    }

    @Test
    fun testSha256AndHmacSha256MatchStandardVectors() {
        val sha256Abc = ProjectHash.sha256("abc".encodeToByteArray())
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Abc,
        )

        // RFC 4231 Test Case 2: Key = "Jefe", Data = "what do ya want for nothing?"
        val hmacRfc4231 = ProjectHash.hmacSha256(
            key = "Jefe".encodeToByteArray(),
            message = "what do ya want for nothing?".encodeToByteArray(),
        ).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            hmacRfc4231,
        )
    }

    @Test
    fun testRoundTripWithCalculationPreservesResultsAndOpensInPreprocessorWithUnlockedTabs() {
        val viewModel = MainViewModel()
        viewModel.calculate()
        val calculatedState = assertIs<CalculationState.Success>(viewModel.uiState.value.calculationState)

        val encoded = viewModel.exportProjectToJson()

        val loadedViewModel = MainViewModel()
        val loadResult = loadedViewModel.loadProjectFromJson(
            jsonString = encoded,
            projectName = "calculated_beam",
        )
        assertTrue(loadResult.isSuccess)

        val loadedState = loadedViewModel.uiState.value
        assertNull(loadedState.fileDialogAlert)
        assertEquals(AppStage.Preprocessor, loadedState.selectedStage)
        assertTrue(loadedState.isCalculated)
        assertTrue(loadedState.isProcessorEnabled)
        assertTrue(loadedState.isPostprocessorEnabled)

        val restoredCalc = assertIs<CalculationState.Success>(loadedState.calculationState)
        assertEquals(calculatedState.displacements, restoredCalc.displacements)
        assertEquals(calculatedState.reactions, restoredCalc.reactions)
        assertEquals(calculatedState.equilibriumResidual, restoredCalc.equilibriumResidual)
        assertEquals(calculatedState.rodStrengths, restoredCalc.rodStrengths)
    }

    @Test
    fun testRoundTripWithInfiniteSafetyFactor() {
        val unloadedConstruction = Construction(
            rods = listOf(Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 2.0, allowableStress = 160.0, distributedLoad = 0.0)),
            nodeForces = listOf(0.0, 0.0),
            fixedNodes = setOf(0),
        )
        val calculation = buildValidSavedCalculation(unloadedConstruction)
        assertEquals(Double.POSITIVE_INFINITY, calculation.rodStrengths[0].safetyFactor)

        val encoded = ProjectFileCodec.encode(unloadedConstruction, calculation)
        val decoded = ProjectFileCodec.decodeProject(encoded)

        assertNull(decoded.calculationInvalidationReason)
        assertNotNull(decoded.calculation)
        assertEquals(Double.POSITIVE_INFINITY, decoded.calculation.rodStrengths[0].safetyFactor)
    }

    @Test
    fun testManualTamperingOfDisplacementsIsDetected() {
        val construction = Construction.defaultDemoConstruction()
        val calculation = buildValidSavedCalculation(construction)
        val encoded = ProjectFileCodec.encode(construction, calculation)

        val firstNonZeroDisplacement = calculation.displacements.first { it != 0.0 }
        val tampered = encoded.replaceFirst(
            firstNonZeroDisplacement.toString(),
            (firstNonZeroDisplacement + 10.0).toString(),
        )

        val decoded = ProjectFileCodec.decodeProject(tampered)
        assertNull(decoded.calculation)
        assertEquals(CalculationInvalidationReason.HashMismatch, decoded.calculationInvalidationReason)
    }

    @Test
    fun testManualTamperingOfIsSatisfiedFlagIsDetected() {
        val overloadedConstruction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 1.0, distributedLoad = 0.0)),
            nodeForces = listOf(0.0, 100.0),
            fixedNodes = setOf(0),
        )
        val calculation = buildValidSavedCalculation(overloadedConstruction)
        assertFalse(calculation.rodStrengths[0].isSatisfied)

        val encoded = ProjectFileCodec.encode(overloadedConstruction, calculation)
        val tampered = encoded.replace("\"isSatisfied\": false", "\"isSatisfied\": true")

        val viewModel = MainViewModel()
        val loadResult = viewModel.loadProjectFromJson(tampered)
        assertTrue(loadResult.isSuccess)
        assertFalse(viewModel.uiState.value.isCalculated)
        assertEquals(CalculationState.Idle, viewModel.uiState.value.calculationState)
        val alert = assertIs<FileDialogAlert.CalculationInvalidated>(viewModel.uiState.value.fileDialogAlert)
        assertEquals(CalculationInvalidationReason.HashMismatch, alert.reason)
    }

    @Test
    fun testManualTamperingOfConstructionLoadWithoutRecalculatingIsDetected() {
        val construction = Construction.defaultDemoConstruction()
        val calculation = buildValidSavedCalculation(construction)
        val encoded = ProjectFileCodec.encode(construction, calculation)

        val tampered = encoded.replaceFirst("-1.0", "-999.0")
        val decoded = ProjectFileCodec.decodeProject(tampered)

        assertNull(decoded.calculation)
        assertEquals(CalculationInvalidationReason.HashMismatch, decoded.calculationInvalidationReason)
    }

    @Test
    fun testResignedForgedCalculationIsCaughtByMathVerification() {
        val construction = Construction.defaultDemoConstruction()
        val validCalc = buildValidSavedCalculation(construction)

        val forgedCalc = validCalc.copy(
            displacements = validCalc.displacements.mapIndexed { idx, v ->
                if (idx == 1) v + 5.0 else v
            },
        )
        // Даже если злоумышленник переподписал файл валидным HMAC:
        val forgedEncoded = ProjectFileCodec.encode(construction, forgedCalc)
        val decoded = ProjectFileCodec.decodeProject(forgedEncoded)

        assertNull(decoded.calculation)
        assertEquals(CalculationInvalidationReason.MathVerificationFailed, decoded.calculationInvalidationReason)
    }

    @Test
    fun testExtractProjectNameSupportsSprwAndJson() {
        assertEquals("bridge_01", ProjectFileDialog.extractProjectName("bridge_01.sprw"))
        assertEquals("bridge_02", ProjectFileDialog.extractProjectName("bridge_02.SPRW"))
        assertEquals("legacy_01", ProjectFileDialog.extractProjectName("legacy_01.json"))
        assertEquals("legacy_02", ProjectFileDialog.extractProjectName("legacy_02.JSON"))
        assertEquals("project", ProjectFileDialog.extractProjectName(".sprw"))
        assertTrue(ProjectFileDialog.isLegacyJsonFileName("old.json"))
        assertTrue(ProjectFileDialog.isLegacyJsonFileName("old.JSON"))
        assertFalse(ProjectFileDialog.isLegacyJsonFileName("new.sprw"))
    }

    @Test
    fun testOpeningLegacyJsonPromptsMandatoryConfirmationAndConvertsOnYes() {
        val legacyJson = """
            {
                "version": 1,
                "rods": [
                    {
                        "length": 2.0,
                        "crossSectionArea": 1.0,
                        "elasticModulus": 1.0,
                        "allowableStress": 160.0,
                        "distributedLoad": 0.0
                    }
                ],
                "nodeForces": [0.0, 5.0],
                "fixedNodes": [0]
            }
        """.trimIndent()

        var savedContent: String? = null
        var saveAsCalls = 0
        val newHandle = ProjectFileHandle()

        val fakeGateway = object : ProjectFileGateway {
            override suspend fun pickProjectFileToOpen(): Result<OpenedProjectFile> =
                Result.success(
                    OpenedProjectFile(
                        file = ProjectFileHandle(),
                        projectName = "legacy_bridge",
                        content = legacyJson,
                        isLegacyJson = true,
                    )
                )

            override suspend fun saveProjectFileAs(
                defaultFileName: String,
                content: String,
            ): Result<SavedProjectFile> {
                saveAsCalls++
                savedContent = content
                return Result.success(
                    SavedProjectFile(
                        file = newHandle,
                        projectName = defaultFileName,
                    )
                )
            }

            override suspend fun overwriteProjectFile(
                file: ProjectFileHandle,
                content: String,
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = MainViewModel(fileGateway = fakeGateway)
        val initialConstruction = viewModel.uiState.value.construction

        // 1. Открываем .json -> появляется диалог подтверждения, текущий проект пока не изменён
        runSuspend { viewModel.executeOpenProjectFromFile() }
        val alert = assertIs<FileDialogAlert.ConfirmLegacyJsonConversion>(viewModel.uiState.value.fileDialogAlert)
        assertEquals("legacy_bridge", alert.projectName)
        assertEquals(initialConstruction, viewModel.uiState.value.construction)
        assertEquals(0, saveAsCalls)

        // 2. Обычный dismiss не закрывает диалог подтверждения
        viewModel.dismissFileDialogAlert()
        assertIs<FileDialogAlert.ConfirmLegacyJsonConversion>(viewModel.uiState.value.fileDialogAlert)

        // 3. Нажатие "Нет" закрывает диалог без изменений
        viewModel.cancelLegacyJsonConversion()
        assertNull(viewModel.uiState.value.fileDialogAlert)
        assertEquals(initialConstruction, viewModel.uiState.value.construction)
        assertEquals(0, saveAsCalls)

        // 4. Снова открываем .json и нажимаем "Да" -> создаётся .sprw и проект загружается
        runSuspend { viewModel.executeOpenProjectFromFile() }
        assertIs<FileDialogAlert.ConfirmLegacyJsonConversion>(viewModel.uiState.value.fileDialogAlert)

        runSuspend { viewModel.executeConfirmLegacyJsonConversion() }
        assertEquals(1, saveAsCalls)
        val exportedSprw = assertNotNull(savedContent)
        assertTrue(exportedSprw.contains("\"formatVersion\": 2"))
        assertNull(viewModel.uiState.value.fileDialogAlert)
        assertEquals("legacy_bridge", viewModel.uiState.value.projectName)
        assertEquals(1, viewModel.uiState.value.construction.rods.size)
        assertEquals(2.0, viewModel.uiState.value.construction.rods[0].length)
    }

    @Test
    fun testHasUnsavedChangesLifecycleWithUndoRedoCalculationAndSave() {
        val fakeHandle = ProjectFileHandle()
        val fakeGateway = object : ProjectFileGateway {
            override suspend fun pickProjectFileToOpen(): Result<OpenedProjectFile>? = null
            override suspend fun saveProjectFileAs(
                defaultFileName: String,
                content: String,
            ): Result<SavedProjectFile> = Result.success(
                SavedProjectFile(file = fakeHandle, projectName = "my_project")
            )

            override suspend fun overwriteProjectFile(
                file: ProjectFileHandle,
                content: String,
            ): Result<Unit> = Result.success(Unit)
        }

        val viewModel = MainViewModel(fileGateway = fakeGateway)

        // 1. Безымянный проект -> индикатор скрыт (hasUnsavedChanges = false)
        assertNull(viewModel.uiState.value.projectName)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // 2. Сохраняем как -> проект чистый
        runSuspend { viewModel.executeSaveProjectAsToFile() }
        assertEquals("my_project", viewModel.uiState.value.projectName)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // 3. Правка конструкции -> появляются несохранённые изменения
        viewModel.addRodRight()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        // 4. Undo к сохранённому состоянию -> индикатор сам гаснет
        viewModel.undo()
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // 5. Redo -> снова есть несохранённые изменения
        viewModel.redo()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        // 6. Сохранение -> снова чисто
        runSuspend { viewModel.executeSaveProjectToFile() }
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // 7. Расчёт (когда файл сохранён без расчёта) -> появляются несохранённые изменения
        viewModel.calculate()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        // 8. Сохранение с расчётом -> снова чисто
        runSuspend { viewModel.executeSaveProjectToFile() }
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // 9. Открытие файла с подменённым расчётом -> расчёт сброшен, состояние отличается от диска
        val encoded = viewModel.exportProjectToJson()
        val tampered = encoded.replace("\"isSatisfied\": true", "\"isSatisfied\": false")
        viewModel.loadProjectFromJson(tampered, projectName = "tampered_project")
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        // 10. Новый проект -> сброс
        viewModel.startNewProject()
        assertNull(viewModel.uiState.value.projectName)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
    }

    private fun runSuspend(block: suspend () -> Unit) {
        var thrown: Throwable? = null
        block.startCoroutine(
            object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) {
                    thrown = result.exceptionOrNull()
                }
            }
        )
        thrown?.let { throw it }
    }
}
