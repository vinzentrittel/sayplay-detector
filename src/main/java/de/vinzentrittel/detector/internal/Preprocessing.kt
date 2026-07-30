package de.vinzentrittel.detector.internal

import org.opencv.imgproc.Imgproc
import org.opencv.ximgproc.EdgeDrawing
import org.opencv.ximgproc.EdgeDrawing_Params as Params
import org.opencv.ximgproc.Ximgproc
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc.COLOR_RGB2HSV

internal val PROCESSING_X_FACTOR = 0.5
internal val PROCESSING_Y_FACTOR = 0.5

internal fun extractRectangle(source: Mat, targetSize: Size): Mat? {
    val lines: List<Line> = identifyLines(source)
    val rectangles: List<Polygon> = getRectangles(lines)
    if (rectangles.isEmpty())
        return null
    val rgbImage = Mat()
    Imgproc.cvtColor(source, rgbImage, Imgproc.COLOR_RGBA2RGB)

    val normalizedRectangle: Mat = stretch(rgbImage, rectangles.first(), targetSize)
    val maxValue: List<Double> = medianBorderValue(normalizedRectangle, 20)
    val singleMinValue: Double = minValue(normalizedRectangle, 20)
    val minValue = List(3) { singleMinValue }
    if (maxValue.fold(false) { meetZero, current -> current == 0.0 || meetZero }) {
        return null
    }

    val smoothRectangle = Mat()
    Imgproc.bilateralFilter(normalizedRectangle, smoothRectangle, 9, 50.0, 50.0)

    return spreadHistogram(
        smoothRectangle,
        minValue,
        maxValue
    )
}

internal fun medianBorderValue(source: Mat, margin: Int = 0): List<Double> {
    val channels = mutableListOf<Mat>()
    val medianIndex = source.width() + source.height() - 2 * margin
    Core.split(source, channels)
    return channels.map { channel ->
        collectBorderValues(channel, margin)
            .sorted()[medianIndex]
    }
}

private fun collectBorderValues(source: Mat, margin: Int = 0): List<Double> {
    val topRow: Mat = source.rowRange(margin, margin + 1)
    val bottomRow: Mat = source.rowRange(source.height() - margin - 1, source.height() - margin)
    val leftColumn: Mat = source.colRange(margin, margin + 1)
    val rightColumn: Mat = source.colRange(source.width() - margin - 1, source.width() - margin)
    val borderMat = Mat()
    Core.hconcat(
        listOf(topRow, bottomRow, leftColumn, rightColumn)
            .map { edge ->
                if(edge.rows() == 1)
                    edge.reshape(1, 1)
                else
                   edge.clone().reshape(1, 1)
            },
        borderMat
    )
    return List(borderMat.cols()) { column -> borderMat.get(0, column)[0] }
}

private fun extractBrightnessChannel(src: Mat, dst: Mat) {
    val tmp = Mat()
    Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2Lab)
    Imgproc.cvtColor(src, tmp, Imgproc.COLOR_RGB2HSV)
    Core.extractChannel(tmp, dst, 2)
    tmp.release()
}

private fun prepareImage(src: Mat): Mat {
    val tmp = Mat()
    var result = Mat()

    Imgproc.resize(
        src,
        result,
        Size(src.cols() * PROCESSING_X_FACTOR, src.rows() * PROCESSING_Y_FACTOR),
        PROCESSING_X_FACTOR,
        PROCESSING_Y_FACTOR,
        Imgproc.INTER_AREA
    )

    extractBrightnessChannel(result, tmp)
    result = spreadHistogram(tmp, 0.0, 255.0)
    result.convertTo(tmp, result.type(), 2.0, -255.0)
    Imgproc.bilateralFilter(tmp, result, 9, 50.0, 50.0)

    tmp.release()
    return result
}

private fun identifyLines(src: Mat): List<Line> {
    val edgeDrawing: EdgeDrawing = Ximgproc.createEdgeDrawing()
    edgeDrawing.setParams(Params().also {
        it._MinLineLength = 50
        it._MaxDistanceBetweenTwoLines = 25.0
        it._LineFitErrorThreshold = 2.0
        it._MaxErrorThreshold = 1.3
        it._GradientThresholdValue = 0
        it._EdgeDetectionOperator = 3
        it._PFmode = true
    })

    val preprocessedSrc: Mat = prepareImage(src)
    edgeDrawing.detectEdges(preprocessedSrc)

    val lineMat = Mat()
    edgeDrawing.detectLines(lineMat)

    return Line.convert(lineMat).map {
        it * Pair(1 / PROCESSING_X_FACTOR, 1 / PROCESSING_Y_FACTOR)
    }
}

internal fun extractParameters(src: Mat): List<Double> {
    val hsvImage = Mat().also {
        Imgproc.cvtColor(src, it, COLOR_RGB2HSV)
    }
    val masks: List<Mat> = thresholdHSVMultiChannel(hsvImage)
    val sections: List<Mat> = masks.flatMap { listOf(it) + split(it) }
    return sections.flatMap { calculateHuMomentsFromBinary(it) }
}