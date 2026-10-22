package ru.sotber.sapr_wolf.preprocessor

import ru.sotber.sapr_wolf.model.Construction
import ru.sotber.sapr_wolf.model.Rod
import kotlin.test.Test
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
}
