package com.jdcr.jdcrbase.number

import kotlin.math.roundToInt

fun Float.to2Decimal(): Float {
    return (this * 100).roundToInt() / 100f
}

fun Float.toInt(): Int {
    return this.roundToInt()
}

fun String?.toInt(): Int? {
    return this?.toIntOrNull()
}