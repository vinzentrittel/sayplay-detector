package de.vinzentrittel.detector.internal

import org.opencv.core.Mat
import org.opencv.core.Point
import java.lang.Math.toRadians
import kotlin.Double.Companion.NaN
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign

class Line private constructor (val a: Point, val b: Point) {

    companion object {
        const val PERPENDICULARITY_THRESHOLD = 5.0

        fun convert(lines: Mat): List<Line> {
            val result: MutableList<Line> = mutableListOf()
            for(lineIndex in 0 until lines.rows()) {
                result.add(Line.create(
                    Point(lines.get(lineIndex, 0)[0], lines.get(lineIndex, 0)[1]),
                    Point(lines.get(lineIndex, 0)[2], lines.get(lineIndex, 0)[3]),
                ))
            }
            return result
        }

        fun create(a: Point, b: Point): Line {
            if (a.x < b.x || (a.x == b.x && a.y < b.y)) {
                return Line(a, b)
            }
            return Line(b, a)
        }

        fun create(a: Point, b: Point, zero: Point): Line {
            return if (squaredDistance(a, zero) < squaredDistance(b, zero)) {
                Line(a, b)
            } else {
                Line(b, a)
            }
        }
    }

    val slope: Double = (b.y - a.y) / (b.x - a.x)
    val yIntercept: Double = a.y - slope * a.x
    val angle: Double = atan(slope) * 180.0 / PI

    fun perpendicular(other: Line, precision: Double = PERPENDICULARITY_THRESHOLD): Boolean {
        return abs(90.0 - abs(angle - other.angle)) < precision
    }

    fun hash(): Int {
        return hash(angle, HashSetting.ANGLE_HASH_PARAMETER)
    }

    fun intersect(other: Line): Point {
        if (this.slope == other.slope) {
            return Point(NaN, NaN)
        }
        if (abs(this.slope) == Double.POSITIVE_INFINITY) {
            return other.intersect(this)
        }
        val x = (
            if (abs(other.slope) == Double.POSITIVE_INFINITY) other.a.x
            else (other.yIntercept - this.yIntercept) / (this.slope - other.slope)
        )
        val y = this.slope * x + this.yIntercept
        return Point(x, y)
    }

    fun squaredDistance(other: Line): Double {
        val squaredDistanceToA = minOf(squaredDistance(a, other.a), squaredDistance(a, other.b))
        val squaredDistanceToB = minOf(squaredDistance(b, other.a), squaredDistance(b, other.b))
        return minOf(squaredDistanceToA, squaredDistanceToB)
    }

    fun similar(to: Line, angleThreshold: Double=3.0, distanceThreshold: Double=10.0): Boolean {
        if (abs(angle - to.angle) > angleThreshold) {
            return false
        }
        val newThis: Line = (
            if (abs(angle) <= 70.0)
                this
            else
                create(Point(a.y, a.x), Point(b.y, b.x))
        )
        val newTo: Line = (
            if (abs(to.angle) <= 70.0)
                to
            else
                create(Point(to.a.y, to.a.x), Point(to.b.y, to.b.x))
        )
        if (abs(newThis.yIntercept - newTo.yIntercept) > distanceThreshold) {
            return false
        }
        val shiftedOther = newTo + (newThis.yIntercept - newTo.yIntercept)
        val aInside = shiftedOther.a.x >= newThis.a.x && shiftedOther.a.x <= newThis.b.x
        val bInside = shiftedOther.b.x >= newThis.a.x && shiftedOther.b.x <= newThis.b.x
        if (aInside || bInside) {
            return true
        }
        return squaredDistance(to) < distanceThreshold * distanceThreshold
    }

    operator fun plus(other: Line): Line {
        val mostDistantPoints: Pair<Point, Point> = listOf(
            Pair(a, other.a),
            Pair(b, other.b),
            Pair(a, other.b),
            Pair(b, other.a)
        ).maxBy { (a, b) -> squaredDistance(a, b) }
        return create(mostDistantPoints.first, mostDistantPoints.second)
    }

    operator fun plus(yOffset: Double): Line {
        return Line(Point(a.x, a.y + yOffset), Point(b.x, b.y + yOffset))
    }

    operator fun times(factors: Pair<Double, Double>): Line {
        return create(a * factors, b * factors)
    }

    fun length(): Double {
        return ((a.x - b.x).pow(2.0) + (a.y - b.y).pow(2.0)).pow(0.5)
    }
    fun newEndPointFromAnchor(zero: Point=Point(0.0, 0.0), length: Double=length()): Point {
        if (a.x == b.x) {
            require(zero.x == a.x) { "Anchor point must be on the line" }

            val x = a.x
            val sign = sign(b.y - a.y)
            val y = zero.y + sign * length
            return Point(x, y)
        }

        require(abs(slope * zero.x + yIntercept - zero.y) < 0.0001) {
            "Anchor point must be on the line. Expected {${zero.x}, ${slope * zero.x + yIntercept}}, got $zero."
        }
        val x = zero.x + length * cos(toRadians(angle))
        return Point(x,slope * x + yIntercept)
    }

    override fun toString(): String {
        return "Line[$a, $b, angle=$angle]"
    }
}