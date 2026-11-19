package ru.sotber.sapr_wolf.ui

import kotlin.math.abs
import kotlin.math.round

internal fun formatCalculationValue(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    if (abs(value) < 1e-9) return "0"
    val rounded = round(value * 10_000.0) / 10_000.0
    if (rounded == 0.0) return value.toString()
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}
