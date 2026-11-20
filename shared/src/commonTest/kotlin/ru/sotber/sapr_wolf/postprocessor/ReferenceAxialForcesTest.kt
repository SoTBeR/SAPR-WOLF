package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.processor.calculateDisplacements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferenceAxialForcesTest {

    private val tolerance = 1e-9

    @Test
    fun testProblem1AxialForces() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 3.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
            ),
            nodeForces = listOf(0.0, 26.0, 0.0),
            fixedNodes = setOf(0, 2),
        )
        assertAxialForces(
            construction = construction,
            expected = listOf(8.0 to 8.0, -18.0 to -18.0),
        )
    }

    @Test
    fun testProblem2AxialForces() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 1.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
            ),
            nodeForces = listOf(0.0, 3.0, 1.0, 0.0),
            fixedNodes = setOf(0, 3),
        )
        assertAxialForces(
            construction = construction,
            expected = listOf(3.0 to 3.0, 0.0 to 0.0, -1.0 to -1.0),
        )
    }

    @Test
    fun testProblem3AxialForces() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
                Rod(length = 1.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
            ),
            nodeForces = listOf(0.0, -4.0, 0.0),
            fixedNodes = setOf(0, 2),
        )
        assertAxialForces(
            construction = construction,
            expected = listOf((2.0 / 5.0) to (-8.0 / 5.0), (12.0 / 5.0) to (12.0 / 5.0)),
        )
    }

    @Test
    fun testProblem4AxialForces() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
            ),
            nodeForces = listOf(0.0, -2.0, 0.0),
            fixedNodes = setOf(0),
        )
        assertAxialForces(
            construction = construction,
            expected = listOf(-1.0 to -1.0, 1.0 to 0.0),
        )
    }

    @Test
    fun testProblem5AxialForces() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = -1.0),
            ),
            nodeForces = listOf(0.0, 0.0, 1.0, 0.0, 0.0),
            fixedNodes = setOf(0, 4),
        )
        assertAxialForces(
            construction = construction,
            expected = listOf(
                (5.0 / 4.0) to (1.0 / 4.0),
                (1.0 / 4.0) to (1.0 / 4.0),
                (-3.0 / 4.0) to (-3.0 / 4.0),
                (-3.0 / 4.0) to (1.0 / 4.0),
            ),
        )
    }

    @Test
    fun testProblem6AxialForces() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 9.0),
                Rod(length = 4.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = -2.0),
            ),
            nodeForces = listOf(0.0, 0.0, 0.0),
            fixedNodes = setOf(0, 2),
        )
        assertAxialForces(
            construction = construction,
            expected = listOf(13.0 to -5.0, -5.0 to 3.0),
        )
    }

    @Test
    fun testSummaryAndReportsContainExpectedSections() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 3.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 10.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 5.0, distributedLoad = 0.0),
            ),
            nodeForces = listOf(0.0, 26.0, 0.0),
            fixedNodes = setOf(0, 2),
        )
        val displacements = calculateDisplacements(construction).getOrThrow()
        val summary = summarizePostprocessor(construction, displacements)

        assertEquals(1, summary.mostLoadedRodIndex)
        assertEquals(listOf(1), summary.overloadedRodIndices)
        assertEquals(18.0, summary.maxAbsAxialForce, tolerance)
        assertEquals(6.0, summary.maxAbsNormalStress, tolerance)
        assertEquals(12.0, summary.maxAbsDisplacement, tolerance)
        assertFalse(analyzeStrength(construction, displacements)[1].isSatisfied)

        val textReport = buildReport(construction, displacements, projectName = "Задача 1", format = ReportFormat.Text)
        assertTrue(textReport.contains("Задача 1"))
        assertTrue(textReport.contains("ИСХОДНЫЕ ДАННЫЕ"))
        assertTrue(textReport.contains("РЕЗУЛЬТАТЫ ПО УЗЛАМ"))
        assertTrue(textReport.contains("НАПРЯЖЁННО-ДЕФОРМИРОВАННОЕ СОСТОЯНИЕ"))
        assertTrue(textReport.contains("ПРОВЕРКА ПРОЧНОСТИ"))
        assertTrue(textReport.contains("ПЕРЕГРУЗ"))

        val htmlReport = buildReport(construction, displacements, projectName = "Задача 1", format = ReportFormat.Html)
        assertTrue(htmlReport.contains("<!DOCTYPE html>"))
        assertTrue(htmlReport.contains("Задача 1"))
        assertTrue(htmlReport.contains("Исходные данные"))
        assertTrue(htmlReport.contains("Проверка прочности"))
        assertTrue(htmlReport.contains("overload"))
    }

    private fun assertAxialForces(
        construction: Construction,
        expected: List<Pair<Double, Double>>,
    ) {
        val displacements = calculateDisplacements(construction).getOrThrow()
        assertEquals(expected.size, construction.rods.size)

        construction.rods.forEachIndexed { rodIndex, rod ->
            val leftSection = calculateSection(construction, displacements, rodIndex, 0.0)
            val rightSection = calculateSection(construction, displacements, rodIndex, rod.length)
            val (expectedStart, expectedEnd) = expected[rodIndex]

            assertEquals(expectedStart, leftSection.axialForce, tolerance, "Стержень ${rodIndex + 1}, x = 0")
            assertEquals(expectedEnd, rightSection.axialForce, tolerance, "Стержень ${rodIndex + 1}, x = L")
        }
    }
}
