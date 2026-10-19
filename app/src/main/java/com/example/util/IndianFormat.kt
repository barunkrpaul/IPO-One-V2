package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Indian digit grouping for rupee amounts: 191100 -> "₹1,91,100", 12.5 -> "₹12.5".
 * Up to [maxDecimals] decimals are kept, trailing zeros are dropped.
 */
fun formatInr(amount: Double, maxDecimals: Int = 2, showPlus: Boolean = false): String {
    val sign = when {
        amount < 0 -> "-"
        showPlus && amount > 0 -> "+"
        else -> ""
    }
    return sign + "₹" + formatIndianNumber(abs(amount), maxDecimals)
}

/** Indian digit grouping without a currency symbol: 1234567.5 -> "12,34,567.5". */
fun formatIndianNumber(value: Double, maxDecimals: Int = 2): String {
    val plain = BigDecimal(abs(value))
        .setScale(maxDecimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    val intPart = plain.substringBefore('.')
    val fraction = plain.substringAfter('.', "")
    return buildString {
        if (value < 0) append('-')
        append(groupIndian(intPart))
        if (fraction.isNotEmpty()) append('.').append(fraction)
    }
}

fun formatIndianNumber(value: Long): String = formatIndianNumber(value.toDouble(), 0)

fun formatInr(amount: Int, showPlus: Boolean = false): String = formatInr(amount.toDouble(), 0, showPlus)

/** Signed percentage with one decimal: 8.333 -> "+8.3%", -2.0 -> "-2.0%". */
fun formatSignedPercent(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return sign + String.format(java.util.Locale.ENGLISH, "%.1f%%", value)
}

private fun groupIndian(digits: String): String {
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    val rest = digits.dropLast(3)
    val pairs = rest.reversed().chunked(2).joinToString(",").reversed()
    return "$pairs,$lastThree"
}
