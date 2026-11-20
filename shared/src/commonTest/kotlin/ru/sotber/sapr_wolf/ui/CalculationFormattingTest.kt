package ru.sotber.sapr_wolf.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculationFormattingTest {

    @Test
    fun formatCalculationValueRoundsHalfAwayFromZeroUpToThreeDecimals() {
        assertEquals("0", formatCalculationValue(0.0))
        assertEquals("0", formatCalculationValue(-0.0))
        assertEquals("0", formatCalculationValue(0.0004))
        assertEquals("0", formatCalculationValue(-0.0004))
        assertEquals("0", formatCalculationValue(1e-5))
        assertEquals("0.001", formatCalculationValue(0.0005))
        assertEquals("-0.001", formatCalculationValue(-0.0005))
        assertEquals("0.003", formatCalculationValue(0.0025))
        assertEquals("-0.003", formatCalculationValue(-0.0025))
        assertEquals("1.001", formatCalculationValue(1.0005))
        assertEquals("2.346", formatCalculationValue(2.3455))
        assertEquals("0.667", formatCalculationValue(2.0 / 3.0))
        assertEquals("1.5", formatCalculationValue(1.5))
        assertEquals("2", formatCalculationValue(2.0))
        assertEquals("-8", formatCalculationValue(-8.0))
        assertEquals("NaN", formatCalculationValue(Double.NaN))
        assertEquals("∞", formatCalculationValue(Double.POSITIVE_INFINITY))
        assertEquals("-∞", formatCalculationValue(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun formatTableValueRoundsHalfAwayFromZeroWithFixedThreeDecimals() {
        assertEquals("0.000", formatTableValue(0.0))
        assertEquals("0.000", formatTableValue(-0.0))
        assertEquals("0.000", formatTableValue(0.0004))
        assertEquals("0.000", formatTableValue(-0.0004))
        assertEquals("0.001", formatTableValue(0.0005))
        assertEquals("-0.001", formatTableValue(-0.0005))
        assertEquals("0.003", formatTableValue(0.0025))
        assertEquals("-0.003", formatTableValue(-0.0025))
        assertEquals("1.001", formatTableValue(1.0005))
        assertEquals("1.500", formatTableValue(1.5))
        assertEquals("-8.000", formatTableValue(-8.0))
        assertEquals("0.667", formatTableValue(2.0 / 3.0))
    }
}
