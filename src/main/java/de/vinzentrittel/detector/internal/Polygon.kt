package de.vinzentrittel.detector.internal

import org.opencv.core.Mat
import org.opencv.core.Point
import kotlin.math.abs

class Polygon(a: Point, b: Point, c: Point, d: Point) {
    val topLeft: Point
    val topRight: Point
    val bottomLeft: Point
    val bottomRight: Point

    init {
        val points = listOf(a, b, c, d)
        val sortedByX = points.sortedBy { point -> point.x }

        val leftPoints = sortedByX.subList(0, 2)
        val rightPoints = sortedByX.subList(2, 4)

        topLeft = leftPoints.maxBy { point -> point.y }
        topRight = rightPoints.maxBy { point -> point.y }
        bottomLeft = leftPoints.minBy { point -> point.y }
        bottomRight = rightPoints.minBy { point -> point.y }
    }

    fun isRectangle(divergenceThreshold: Double=10.0, perpendicularityThreshold: Double=Line.PERPENDICULARITY_THRESHOLD): Boolean {
        if (
            abs(topLeft.x - topRight.x) - abs(bottomLeft.x - bottomRight.x) > divergenceThreshold
            || abs(topLeft.y - bottomLeft.y) - abs(topRight.y - bottomRight.y) > divergenceThreshold
        ) {
            return false
        }
        return Line.create(topLeft, topRight)
            .perpendicular(
                Line.create(topLeft, bottomLeft),
                perpendicularityThreshold
            )
    }

    fun approximateArea(): Double {
        return abs((topRight.x - topLeft.x) * (topRight.y - bottomRight.y))
    }

    companion object {
        private fun from2Lines(a: Line, b: Line, perpendicularityThreshold: Double): Polygon {
            val intersection: Point = a.intersect(b)
            val directedA = Line.create(a.a, a.b, intersection)
            val directedB = Line.create(b.a, b.b, intersection)

            val aOffset = Point(directedA.b.x - directedA.a.x, directedA.b.y - directedA.a.y)
            val bOffset = Point(directedB.b.x - directedB.a.x, directedB.b.y - directedB.a.y)

            return Polygon(
                intersection, intersection + aOffset, intersection + bOffset, intersection + aOffset + bOffset
            )
        }

        private fun from3Lines(
            a: Line, b: Line, c: Line, perpendicularityThreshold: Double
        ): Polygon {
            val leftPoint = a.intersect(b)
            val rightPoint = b.intersect(c)
            val directedA = Line.create(a.a, a.b, leftPoint)
            val directedC = Line.create(c.a, c.b, rightPoint)
            val aSign = if (directedA.a == a.a) 1 else -1
            val cSign = if (directedC.a == c.a) 1 else -1

            val maxLength = maxOf(directedA.length(), directedC.length())
            val endPointOfA = a.newEndPointFromAnchor(leftPoint, maxLength * aSign)
            val endPointOfC = c.newEndPointFromAnchor(rightPoint, maxLength * cSign)

            return Polygon(
                endPointOfA, leftPoint, rightPoint, endPointOfC
            )
        }

        private fun from4Lines(
            a: Line, b: Line, c: Line, d: Line, perpendicularityThreshold: Double
        ): Polygon {
            return Polygon(
                a.intersect(b),
                b.intersect(c),
                c.intersect(d),
                d.intersect(a)
            )
        }

        fun fromLines(
            a: Line, b: Line, c: Line?=null, d: Line?=null,
            perpendicularityThreshold: Double=Line.PERPENDICULARITY_THRESHOLD
        ): Polygon {

            if (c == null && d == null) {
                return from2Lines(a, b, perpendicularityThreshold)
            }
            if (d == null) {
                return from3Lines(a, b, c!!, perpendicularityThreshold)
            }

            return from4Lines(a, b, c!!, d, perpendicularityThreshold)
        }
    }
}