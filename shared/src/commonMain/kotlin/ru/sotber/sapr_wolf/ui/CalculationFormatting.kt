package ru.sotber.sapr_wolf.ui

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

private const val ROUNDING_EPS = 1e-12

private fun roundHalfAwayFromZeroScaled(absValue: Double, factor: Long): Long? {
    val scaledDouble = absValue * factor.toDouble()
    if (!scaledDouble.isFinite() || scaledDouble >= (Long.MAX_VALUE / 2).toDouble()) {
        return null
    }
    return floor(scaledDouble + 0.5 + ROUNDING_EPS).toLong()
}

internal fun formatCalculationValue(value: Double, decimals: Int = 3): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    require(decimals >= 0) { "decimals must be non-negative" }
    val factor = 10.0.pow(decimals).toLong()
    val scaled = roundHalfAwayFromZeroScaled(abs(value), factor) ?: return value.toString()
    if (scaled == 0L) return "0"
    val sign = if (value < 0.0) "-" else ""
    if (decimals == 0) return "$sign$scaled"
    val integerPart = scaled / factor
    val fractionalPart = (scaled % factor)
        .toString()
        .padStart(decimals, '0')
        .trimEnd('0')
    return if (fractionalPart.isEmpty()) {
        "$sign$integerPart"
    } else {
        "$sign$integerPart.$fractionalPart"
    }
}

internal fun formatTableValue(value: Double, decimals: Int = 3): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    require(decimals >= 0) { "decimals must be non-negative" }
    val factor = 10.0.pow(decimals).toLong()
    val scaled = roundHalfAwayFromZeroScaled(abs(value), factor) ?: return value.toString()
    if (scaled == 0L) {
        return if (decimals == 0) "0" else "0." + "0".repeat(decimals)
    }
    val sign = if (value < 0.0) "-" else ""
    if (decimals == 0) return "$sign$scaled"
    val integerPart = scaled / factor
    val fractionalPart = (scaled % factor).toString().padStart(decimals, '0')
    return "$sign$integerPart.$fractionalPart"
}
