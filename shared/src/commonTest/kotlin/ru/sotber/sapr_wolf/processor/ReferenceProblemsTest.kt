package ru.sotber.sapr_wolf.processor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
