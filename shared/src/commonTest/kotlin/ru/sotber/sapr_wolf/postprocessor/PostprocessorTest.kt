package ru.sotber.sapr_wolf.postprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import ru.sotber.sapr_wolf.processor.calculateDisplacements
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostprocessorTest {

    @Test
    fun testReferenceProblem1Postprocessor() {
        // Задача 1: стержни (3, 2, 0); (2, 3, 0), опоры 0, 2, F_1 = 26
        val construction = Construction(
            rods = listOf(
                Rod(length = 3.0, crossSectionArea = 2.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0),
                Rod(length = 2.0, crossSectionArea = 3.0, elasticModulus = 1.0, allowableStress = 100.0, distributedLoad = 0.0)
            ),
            nodeForces = listOf(0.0, 26.0, 0.0),
            fixedNodes = setOf(0, 2)
        )

        val displacements = calculateDisplacements(construction).getOrThrow()
        val reactions = calculateReactions(construction, displacements)

        // Реакции опор
        assertEquals(-8.0, reactions[0]!!, 1e-9, "Реакция в опоре 0")
        assertEquals(-18.0, reactions[2]!!, 1e-9, "Реакция в опоре 2")

        // Проверка равновесия: R_0 + R_2 + F_1 == 0 (-8 - 18 + 26 == 0)
        val residual = calculateEquilibriumResidual(construction, reactions)
        assertTrue(abs(residual) < 1e-9, "Невязка статического равновесия: $residual")

        // Сечения стержня 1 (N = 8, σ = 4)
        val rod0Start = calculateSection(construction, displacements, 0, 0.0)
        val rod0End = calculateSection(construction, displacements, 0, 3.0)
        assertEquals(8.0, rod0Start.axialForce, 1e-9)
        assertEquals(8.0, rod0End.axialForce, 1e-9)
        assertEquals(4.0, rod0Start.normalStress, 1e-9)
        assertEquals(4.0, rod0End.normalStress, 1e-9)

        // Сечения стержня 2 (N = -18, σ = -6)
        val rod1Start = calculateSection(construction, displacements, 1, 0.0)
        val rod1End = calculateSection(construction, displacements, 1, 2.0)
        assertEquals(-18.0, rod1Start.axialForce, 1e-9)
        assertEquals(-18.0, rod1End.axialForce, 1e-9)
        assertEquals(-6.0, rod1Start.normalStress, 1e-9)
        assertEquals(-6.0, rod1End.normalStress, 1e-9)

        // Анализ прочности
        val strength = analyzeStrength(construction, displacements)
        assertEquals(2, strength.size)
        assertTrue(strength[0].isSatisfied)
        assertEquals(25.0, strength[0].safetyFactor, 1e-9) // 100 / 4
        assertTrue(strength[1].isSatisfied)
        assertEquals(100.0 / 6.0, strength[1].safetyFactor, 1e-9) // 100 / 6
    }

    @Test
    fun testEquilibriumOnAllReferenceProblems() {
        val problems = listOf(
            // Проблема 1
            Construction(
                rods = listOf(
                    Rod(3.0, 2.0, 1.0, 100.0, 0.0),
                    Rod(2.0, 3.0, 1.0, 100.0, 0.0)
                ),
                nodeForces = listOf(0.0, 26.0, 0.0),
                fixedNodes = setOf(0, 2)
            ),
            // Проблема 2
            Construction(
                rods = listOf(
                    Rod(1.0, 3.0, 1.0, 100.0, 0.0),
                    Rod(2.0, 2.0, 1.0, 100.0, 0.0),
                    Rod(1.0, 1.0, 1.0, 100.0, 0.0)
                ),
                nodeForces = listOf(0.0, 3.0, 1.0, 0.0),
                fixedNodes = setOf(0, 3)
            ),
            // Проблема 3
            Construction(
                rods = listOf(
                    Rod(2.0, 1.0, 1.0, 100.0, 1.0),
                    Rod(1.0, 2.0, 1.0, 100.0, 0.0)
                ),
                nodeForces = listOf(0.0, -4.0, 0.0),
                fixedNodes = setOf(0, 2)
            ),
            // Проблема 4
            Construction(
                rods = listOf(
                    Rod(2.0, 2.0, 1.0, 100.0, 0.0),
                    Rod(1.0, 1.0, 1.0, 100.0, 1.0)
                ),
                nodeForces = listOf(0.0, -2.0, 0.0),
                fixedNodes = setOf(0)
            ),
            // Проблема 5
            Construction(
                rods = listOf(
                    Rod(1.0, 1.0, 1.0, 100.0, 1.0),
                    Rod(2.0, 2.0, 1.0, 100.0, 0.0),
                    Rod(2.0, 2.0, 1.0, 100.0, 0.0),
                    Rod(1.0, 1.0, 1.0, 100.0, -1.0)
                ),
                nodeForces = listOf(0.0, 0.0, 1.0, 0.0, 0.0),
                fixedNodes = setOf(0, 4)
            ),
            // Проблема 6
            Construction(
                rods = listOf(
                    Rod(2.0, 2.0, 1.0, 100.0, 9.0),
                    Rod(4.0, 1.0, 1.0, 100.0, -2.0)
                ),
                nodeForces = listOf(0.0, 0.0, 0.0),
                fixedNodes = setOf(0, 2)
            )
        )

        for ((index, problem) in problems.withIndex()) {
            val displacements = calculateDisplacements(problem).getOrThrow()
            val reactions = calculateReactions(problem, displacements)
            val residual = calculateEquilibriumResidual(problem, reactions)
            assertTrue(
                abs(residual) < 1e-9,
                "Равновесие нарушено в эталонной задаче ${index + 1}: невязка = $residual"
            )
        }
    }
}
