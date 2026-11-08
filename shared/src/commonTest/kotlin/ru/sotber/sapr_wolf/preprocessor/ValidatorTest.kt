package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.ui.FileDialogAlert
import ru.sotber.sapr_wolf.ui.MainViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidatorTest {

    private fun assertHasCode(construction: Construction, expectedCode: String) {
        val issues = ConstructionValidator.validate(construction)
        assertTrue(
            issues.any { it.code == expectedCode },
            "Ожидался диагностический код $expectedCode, но получены: ${issues.map { it.code }}"
        )
    }

    @Test
    fun testE01NoRods() {
        val construction = Construction(rods = emptyList())
        assertHasCode(construction, "E01")
        assertTrue(ConstructionValidator.hasErrors(construction))
    }

    @Test
    fun testE02InvalidLength() {
        val construction = Construction(
            rods = listOf(Rod(length = -1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0)),
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "E02")
    }

    @Test
    fun testE03InvalidArea() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 0.0, elasticModulus = 1.0, allowableStress = 100.0)),
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "E03")
    }

    @Test
    fun testE04InvalidModulus() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = -5.0, allowableStress = 100.0)),
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "E04")
    }

    @Test
    fun testE05InvalidAllowableStress() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 0.0)),
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "E05")
    }

    @Test
    fun testE06NoSupports() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0)),
            fixedNodes = emptySet()
        )
        assertHasCode(construction, "E06")
    }

    @Test
    fun testE07SupportIndexOutOfRange() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0)),
            fixedNodes = setOf(0, 5) // Допустимы только 0 и 1
        )
        assertHasCode(construction, "E07")
    }

    @Test
    fun testE08NaNOrInfinity() {
        val construction = Construction(
            rods = listOf(Rod(length = Double.NaN, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0)),
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "E08")
    }

    @Test
    fun testE09ForcesSizeMismatch() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0)),
            nodeForces = listOf(1.0), // Нужно 2 силы для 1 стержня
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "E09")
    }

    @Test
    fun testW01NoLoads() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0)),
            nodeForces = listOf(0.0, 0.0),
            fixedNodes = setOf(0)
        )
        assertHasCode(construction, "W01")
    }

    @Test
    fun testW02ForceAtSupport() {
        val construction = Construction(
            rods = listOf(Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0)),
            nodeForces = listOf(10.0, 0.0),
            fixedNodes = setOf(0) // В узле 0 опора и сила 10
        )
        assertHasCode(construction, "W02")
    }

    @Test
    fun testNormalizerFixesInvalidParametersAndGarbage() {
        val raw = Construction(
            rods = listOf(
                Rod(
                    length = -2.0,
                    crossSectionArea = 0.0,
                    elasticModulus = Double.NaN,
                    allowableStress = -10.0,
                    distributedLoad = Double.POSITIVE_INFINITY,
                ),
                Rod(
                    length = 2.0,
                    crossSectionArea = 3.0,
                    elasticModulus = 4.0,
                    allowableStress = 200.0,
                    distributedLoad = 5.0,
                ),
            ),
            nodeForces = listOf(10.0), // Недостаточно сил (нужно 3 для 2 стержней)
            fixedNodes = setOf(-1, 1, 99), // -1 и 99 вне диапазона, 1 — промежуточная валидная опора
        )

        val result = ConstructionNormalizer.normalize(raw)
        assertTrue(result.wasModified)

        val normalized = result.construction
        assertEquals(2, normalized.rods.size)
        assertEquals(
            Rod(
                length = ConstructionNormalizer.DEFAULT_LENGTH,
                crossSectionArea = ConstructionNormalizer.DEFAULT_AREA,
                elasticModulus = ConstructionNormalizer.DEFAULT_MODULUS,
                allowableStress = ConstructionNormalizer.DEFAULT_ALLOWABLE_STRESS,
                distributedLoad = ConstructionNormalizer.DEFAULT_DISTRIBUTED_LOAD,
            ),
            normalized.rods[0],
        )
        assertEquals(raw.rods[1], normalized.rods[1])
        assertEquals(listOf(10.0, 0.0, 0.0), normalized.nodeForces)
        assertEquals(setOf(1), normalized.fixedNodes)
        assertFalse(ConstructionValidator.hasErrors(normalized))
    }

    @Test
    fun testNormalizerClearsGarbageWhenNoRods() {
        val raw = Construction(
            rods = emptyList(),
            nodeForces = listOf(1.0, 2.0),
            fixedNodes = setOf(0, 1),
        )
        val result = ConstructionNormalizer.normalize(raw)
        assertTrue(result.wasModified)
        assertTrue(result.construction.nodeForces.isEmpty())
        assertTrue(result.construction.fixedNodes.isEmpty())
        assertTrue(result.changes.contains(NormalizationChange.ClearedGarbageForEmptyRods))
    }

    @Test
    fun testViewModelLoadProjectTriggersAlertWhenNormalized() {
        val viewModel = MainViewModel()
        val jsonWithIssues = """
            {
                "version": 1,
                "rods": [
                    {
                        "length": -5.0,
                        "crossSectionArea": 1.0,
                        "elasticModulus": 1.0,
                        "allowableStress": 160.0,
                        "distributedLoad": 0.0
                    }
                ],
                "nodeForces": [1.0, 2.0, 3.0, 4.0],
                "fixedNodes": [0, 10]
            }
        """.trimIndent()

        val loadResult = viewModel.loadProjectFromJson(jsonWithIssues)
        assertTrue(loadResult.isSuccess)

        val state = viewModel.uiState.value
        val alert = state.fileDialogAlert
        assertIs<FileDialogAlert.NormalizedOnLoad>(alert)
        assertEquals(3, alert.changes.size)
        assertEquals(1.0, state.construction.rods[0].length)
        assertEquals(listOf(1.0, 2.0), state.construction.nodeForces)
        assertEquals(setOf(0), state.construction.fixedNodes)

        viewModel.dismissFileDialogAlert()
        assertNull(viewModel.uiState.value.fileDialogAlert)
    }

    @Test
    fun testViewModelLoadInvalidJsonShowsLoadErrorAlert() {
        val viewModel = MainViewModel()
        val brokenJson = "{ invalid_json: [1, 2"

        val loadResult = viewModel.loadProjectFromJson(brokenJson)
        assertTrue(loadResult.isFailure)
        assertIs<FileDialogAlert.LoadError>(viewModel.uiState.value.fileDialogAlert)
    }
}
