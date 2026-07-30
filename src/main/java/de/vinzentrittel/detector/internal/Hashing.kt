package de.vinzentrittel.detector.internal

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class HashSetting private constructor (val parameters: List<HashParameter>) {
    companion object {
        val ANGLE_HASH_PARAMETER = HashParameter(-90.0, 90.0, 40)
        val INTERSECTION_HASH_PARAMETER = HashParameter(-1024.0, 2048.0, 300)
        val LINE_HASH_SETTING: HashSetting = HashSetting(listOf(INTERSECTION_HASH_PARAMETER, ANGLE_HASH_PARAMETER))
    }

    init {
        require(parameters.isNotEmpty()) { "parameters must not be empty" }
    }
}
class HashParameter(val min: Double, max: Double, val binCount: Int) {
    init {
        require(min < max) { "min must be smaller than max" }
        require(binCount > 0) { "granularity must be positive" }
    }

    val size: Double = abs(max - min)
    val binSize: Double = (size / binCount)
}

fun hash(value: Double, setting: HashParameter): Int {
    return floor((value - setting.min) / setting.size * setting.binCount + 1e-9).toInt() % setting.binCount
}

fun hash(values: List<Double>, setting: HashSetting): Int {
    require(values.size == setting.parameters.size) { "values and setting must have the same size" }

    var result = 0
    for ((value, parameter) in values.zip(setting.parameters)) {
        result = result * parameter.binCount + hash(value, parameter)
    }
    return result
}

fun hash(vararg values: Double, setting: HashSetting): Int {
    return hash(values.toList(), setting)
}

fun unhash(hash: Int, setting: HashParameter): Double {
    return hash * setting.binSize + setting.min
}

fun unhash(hash: Int, setting: HashSetting): List<Double> {
    require(hash >= 0) { "hash must be positive" }
    require(setting.parameters.isNotEmpty())

    val result: MutableList<Double> = mutableListOf()
    var remaining = hash
    for (parameter in setting.parameters.reversed()) {
        result.add(unhash(remaining % parameter.binCount, parameter))
        remaining /= parameter.binCount
    }
    return result.reversed()
}