package ru.sotber.sapr_wolf.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumericTextFieldTest {

    @Test
    fun partialNumericInputRejectsDisallowedMinusAndMalformedNumbers() {
        assertTrue(isValidPartialNumericInput("", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("123.45", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("123,45", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput(".5", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("2.5e-3", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("1e", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("1e-", allowNegative = false, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("1E+5", allowNegative = false, allowScientificNotation = true))

        assertFalse(isValidPartialNumericInput("-", allowNegative = false, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("-1", allowNegative = false, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("1-2", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("1..2", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("1.2,3", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("1ee2", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("e5", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("12a", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("1e5", allowNegative = true, allowScientificNotation = false))
    }

    @Test
    fun partialNumericInputAllowsLeadingMinusWhenNegativeEnabled() {
        assertTrue(isValidPartialNumericInput("-", allowNegative = true, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("-.", allowNegative = true, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("-12.5", allowNegative = true, allowScientificNotation = true))
        assertTrue(isValidPartialNumericInput("-2.5e-3", allowNegative = true, allowScientificNotation = true))
        assertFalse(isValidPartialNumericInput("--1", allowNegative = true, allowScientificNotation = true))
    }

    @Test
    fun formatEditableNumberStripsTrailingPointZero() {
        assertEquals("0", formatEditableNumber(0.0))
        assertEquals("0", formatEditableNumber(-0.0))
        assertEquals("1", formatEditableNumber(1.0))
        assertEquals("-25", formatEditableNumber(-25.0))
        assertEquals("1.25", formatEditableNumber(1.25))
    }
}
