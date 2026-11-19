package ru.sotber.sapr_wolf.processor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.LinearSystem
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.postprocessor.calculateEquilibriumResidual
import ru.sotber.sapr_wolf.postprocessor.calculateReactions
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferenceProblemsTest {

    private fun assertDisplacementsEqual(expected: DoubleArray, actual: DoubleArray, tolerance: Double = 1e-9) {
        assertEquals(expected.size, actual.size, "Размеры векторов перемещений не совпадают")
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i], tolerance, "Несовпадение в узле $i")
        }
    }

    @Test
    fun testProblem1() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 3.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0)
            ),
            nodeForces = listOf(0.0, 26.0, 0.0),
            fixedNodes = setOf(0, 2)
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isSuccess)
        assertDisplacementsEqual(doubleArrayOf(0.0, 12.0, 0.0), result.getOrThrow())
    }

    @Test
    fun testProblem2() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 1.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0)
            ),
            nodeForces = listOf(0.0, 3.0, 1.0, 0.0),
            fixedNodes = setOf(0, 3)
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isSuccess)
        assertDisplacementsEqual(doubleArrayOf(0.0, 1.0, 1.0, 0.0), result.getOrThrow())
    }

    @Test
    fun testProblem3() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
                Rod(length = 1.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0)
            ),
            nodeForces = listOf(0.0, -4.0, 0.0),
            fixedNodes = setOf(0, 2)
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isSuccess)
        assertDisplacementsEqual(doubleArrayOf(0.0, -1.2, 0.0), result.getOrThrow())
    }

    @Test
    fun testProblem4() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0)
            ),
            nodeForces = listOf(0.0, -2.0, 0.0),
            fixedNodes = setOf(0)
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isSuccess)
        assertDisplacementsEqual(doubleArrayOf(0.0, -1.0, -0.5), result.getOrThrow())
    }

    @Test
    fun testProblem5() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = -1.0)
            ),
            nodeForces = listOf(0.0, 0.0, 1.0, 0.0, 0.0),
            fixedNodes = setOf(0, 4)
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isSuccess)
        assertDisplacementsEqual(doubleArrayOf(0.0, 0.75, 1.0, 0.25, 0.0), result.getOrThrow())
    }

    @Test
    fun testProblem6() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 9.0),
                Rod(length = 4.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = -2.0)
            ),
            nodeForces = listOf(0.0, 0.0, 0.0),
            fixedNodes = setOf(0, 2)
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isSuccess)
        assertDisplacementsEqual(doubleArrayOf(0.0, 4.0, 0.0), result.getOrThrow())
    }

    @Test
    fun testSingularSystemNoSupports() {
        val construction = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
            ),
            nodeForces = listOf(0.0, 10.0, 0.0),
            fixedNodes = emptySet(),
        )

        val result = calculateDisplacements(construction)
        assertTrue(result.isFailure, "Система без опор должна возвращать ошибку")

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.isNotBlank(), "Сообщение об ошибке не должно быть пустым")
        assertFalse(message.contains("NaN"), "Сообщение об ошибке не должно содержать NaN")
    }

    @Test
    fun testSingularSystemDegenerateMatrix() {
        val degenerateSystem = LinearSystem(
            matrix = arrayOf(
                doubleArrayOf(1.0, -1.0),
                doubleArrayOf(-1.0, 1.0),
            ),
            rightPart = doubleArrayOf(5.0, -5.0),
        )

        val error = assertFailsWith<IllegalStateException> {
            solveByGauss(degenerateSystem)
        }
        assertTrue(error.message?.isNotBlank() == true)
    }

    @Test
    fun testInteriorSupportSplitsSystemIntoIndependentParts() {
        // Конструкция с промежуточной опорой в узле 2:
        // Левая часть (стержни 0, 1; опора в узлах 0 и 2) и правая часть (стержни 2, 3; опора в узле 2)
        val leftPart = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
                Rod(length = 1.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
            ),
            nodeForces = listOf(0.0, -4.0, 0.0),
            fixedNodes = setOf(0, 2),
        )

        val rightPart = Construction(
            rods = listOf(
                Rod(length = 2.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 1.0, crossSectionArea = 1.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 1.0),
            ),
            nodeForces = listOf(0.0, -2.0, 0.0),
            fixedNodes = setOf(0),
        )

        val combined = Construction(
            rods = leftPart.rods + rightPart.rods,
            nodeForces = listOf(0.0, -4.0, 5.0, -2.0, 0.0),
            fixedNodes = setOf(0, 2),
        )

        val leftDisplacements = calculateDisplacements(leftPart).getOrThrow()
        val rightDisplacements = calculateDisplacements(rightPart).getOrThrow()
        val combinedDisplacements = calculateDisplacements(combined).getOrThrow()

        assertDisplacementsEqual(
            expected = doubleArrayOf(
                leftDisplacements[0],
                leftDisplacements[1],
                0.0,
                rightDisplacements[1],
                rightDisplacements[2],
            ),
            actual = combinedDisplacements,
        )

        val leftReactions = calculateReactions(leftPart, leftDisplacements)
        val rightReactions = calculateReactions(rightPart, rightDisplacements)
        val combinedReactions = calculateReactions(combined, combinedDisplacements)

        assertEquals(leftReactions[0]!!, combinedReactions[0]!!, 1e-9)
        // В узле 2 приложена внешняя сила F = 5.0 прямо на опору, поэтому она целиком уходит в реакцию (-5.0)
        assertEquals(leftReactions[2]!! + rightReactions[0]!! - 5.0, combinedReactions[2]!!, 1e-9)

        val residual = calculateEquilibriumResidual(combined, combinedReactions)
        assertTrue(abs(residual) < 1e-9, "Невязка равновесия составной системы: $residual")
    }
}
