package de.vinzentrittel.detector.internal

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Moments

private const val THRESHOLD_VALUE = 200.0
private const val OUTPUT_WIDTH = 400
private const val OUTPUT_HEIGHT = 560

fun stretch(source: Mat, rectangle: Polygon, size: Size): Mat {
    val from = MatOfPoint2f(
        rectangle.bottomLeft, rectangle.bottomRight, rectangle.topRight, rectangle.topLeft
    )
    val to = MatOfPoint2f(
        Point(0.0, 0.0),
        Point(size.width - 1.0, 0.0),
        Point(size.width - 1.0, size.height - 1.0),
        Point(0.0, size.height - 1.0)
    )
    val transform: Mat = Imgproc.getPerspectiveTransform(from, to)
    val transformed = Mat()
    Imgproc.warpPerspective(source, transformed, transform, size)
    return transformed
}

internal fun spreadHistogram(source: Mat, minValue: List<Double>, maxValue: List<Double>): Mat {
    require(source.channels() == minValue.size && source.channels() == maxValue.size) {
        "Channel mismatch. Expected ${source.channels()}, got ${minValue.size} and ${maxValue.size} components./n" +
        "Do you meant to call 'spreadHistogram(Mat, Double, Double)', that provides broadcasting?"
    }
    require(minValue.zip(maxValue)
        .fold(true) { acc, (min, max) -> acc && min < max}
    ) {
        "'minValue' is not strictly less than 'maxValue' ($minValue !< $maxValue)"
    }

    val inChannels = MutableList(source.channels()) { Mat() }
    val outChannels = MutableList(source.channels()) { Mat() }
    Core.split(source, inChannels)
    for ( channel in 0 until inChannels.size ) {
        val alpha = 255.0f / (maxValue[channel] - minValue[channel])
        val beta = -minValue[channel] * alpha
        inChannels[channel].convertTo(outChannels[channel], CvType.CV_8U, alpha, beta)
    }
    val result = Mat()
    Core.merge(outChannels, result)
    return result
}

internal fun spreadHistogram(source: Mat, minValue: Double, maxValue: Double): Mat {
    return spreadHistogram(
        source,
        List(source.channels()) { minValue },
        List(source.channels()) { maxValue }
    )
}

internal fun minValue(source: Mat, margin: Int): Double {
    return Core.minMaxLoc(source.submat(
        margin, source.width() - margin, margin, source.height() - margin
    ).reshape(1)).minVal
}

private fun composeLut(binCount: Int, endValue: Int = 256): Mat {
    val binSize: Int = endValue / binCount
    return Mat(1, 256, CvType.CV_8U)
        .also { lut ->
            lut.put(0, 0, ByteArray(256) {
                if (it < endValue)
                    (it / binSize).toByte()
                else
                    0
            })
        }
}

private const val HUE_BIN_COUNT = 6
private const val SATURATION_BIN_COUNT = 2
private const val VALUE_BIN_COUNT = 3
private val HUE_THRESHOLD_LUT: Mat = composeLut(HUE_BIN_COUNT, 180)
private val SATURATION_THRESHOLD_LUT: Mat = composeLut(SATURATION_BIN_COUNT)
private val VALUE_THRESHOLD_LUT: Mat = composeLut(VALUE_BIN_COUNT)

internal fun thresholdHSVMultiChannel(source: Mat): List<Mat> {
    fun computeChannelMasks(channel: Mat, lut: Mat, binCount: Int): List<Mat> = Mat()
        .let { labels ->
            Core.LUT(channel, lut, labels)
            List(binCount) { index: Int ->
                Mat()
                    .also { mask: Mat ->
                        Core.compare(labels, Scalar(index.toDouble()), mask, Core.CMP_EQ)
                    }
            }
        }

    val channels = mutableListOf<Mat>()
    Core.split(source, channels)

    return computeChannelMasks(channels[0], HUE_THRESHOLD_LUT, HUE_BIN_COUNT) +
            computeChannelMasks(channels[1], SATURATION_THRESHOLD_LUT, SATURATION_BIN_COUNT) +
            computeChannelMasks(channels[2], VALUE_THRESHOLD_LUT, VALUE_BIN_COUNT)
}

internal fun calculateHuMomentsFromBinary(source: Mat): List<Double> {
    /**
     * @return Hu moments of [source]. The returned Mat is of size (7, 1) and type
     *         CV_64F.
     * @param[source] single channel binary image.
     */
    val moments: Moments = Imgproc.moments(source, true)
    val huMoments = Mat().also {
        Imgproc.HuMoments(moments, it)
    }
    return List(7) { huMoments.get(it, 0)[0] }
}

internal fun split(src: Mat): List<Mat> {
    /**
     * @param[src] single channel image
     *
     * @return an image one quarter the size of [src] (floor(height / 2) x floor(width / 2)). Each
     * channel contains one spacial quarter of [src].
     */
    val newWidth = src.cols() / 2
    val newHeight = src.rows() / 2

    return listOf(
        src.submat(0, newHeight, 0, newWidth),
        src.submat(newHeight, 2 * newHeight, 0, newWidth),
        src.submat(0, newHeight, newWidth, 2 * newWidth),
        src.submat(newHeight, 2 * newHeight, newWidth, 2 * newWidth)
    )
}