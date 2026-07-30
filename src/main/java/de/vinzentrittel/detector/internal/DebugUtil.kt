package de.vinzentrittel.detector.internal

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

internal fun drawPolygons(src: Mat, polygons: List<Polygon>, color: Scalar = Scalar(255.0, 0.0, 0.0), thickness: Int = 3): Mat {
    val result = src.clone()
    for(polygon in polygons) {
        val points = MatOfPoint(
            *listOf(
                polygon.topLeft,
                polygon.topRight,
                polygon.bottomRight,
                polygon.bottomLeft
            ).toTypedArray()
        )
        Imgproc.polylines(
            result, listOf(points),
            true, color, thickness
        )
    }
    return result
}

internal fun drawFrame(source: Mat, color: Scalar = Scalar(255.0, 0.0, 0.0)): Mat {
    val rotatedImage = Mat()
    Core.rotate(
        extractRectangle(source, Size(512.0, 512.0))
            ?: source,
        rotatedImage,
        Core.ROTATE_90_CLOCKWISE
    )
    if (rotatedImage.width() == 512) {
        val parameterCount = extractParameters(rotatedImage).size
        Log.d("masks", "$parameterCount")
    }
    return rotatedImage
}

internal fun rotateRectangles(rectangles: List<Polygon>, height: Double): List<Polygon> {

    return rectangles.map {
        Polygon(
            Point(height - it.topLeft.y, it.topLeft.x),
            Point(height - it.topRight.y, it.topRight.x),
            Point(height - it.bottomLeft.y, it.bottomLeft.x),
            Point(height - it.bottomRight.y, it.bottomRight.x)
        )
    }
}

internal fun drawLines(
    src: Mat,
    lines: List<Line>,
    color: Scalar = Scalar(255.0, 0.0, 0.0),
    thickness: Int = 3,
    type: Int = 8
): Mat {
    val result = src.clone()
    for(line in lines) {
        Imgproc.line(
            result, line.a, line.b, color, thickness, type
        )
    }
    return result
}

internal fun drawLines(
    src: Mat,
    lines: Mat,
    color: Scalar = Scalar(255.0, 0.0, 0.0),
    thickness: Int = 3,
    type: Int = 8
): Mat {
    val lineList = emptyList<Line>().toMutableList()
    for(lineIndex in 0 until lines.rows()) {
        lineList.add(Line.create(
            Point(lines.get(lineIndex, 0)[0], lines.get(lineIndex, 0)[1]),
            Point(lines.get(lineIndex, 0)[2], lines.get(lineIndex, 0)[3])
        ))
    }

    return drawLines(src, lineList, color, thickness, type)
}

internal fun toLines(lineMap: LineMap): List<Line> {
    val result = mutableListOf<Line>()
    for (lines in lineMap.values) {
        result.addAll(lines)
    }
    return result
}