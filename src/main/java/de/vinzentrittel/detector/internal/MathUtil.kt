package de.vinzentrittel.detector.internal

import org.opencv.core.Point

fun squaredDistance(a: Point, b: Point): Double {
    return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
}

operator fun Point.plus(other: Point): Point {
    return Point(x + other.x, y + other.y)
}

operator fun Point.times(factors: Pair<Double, Double>): Point {
    return Point(x * factors.first, y * factors.second)
}