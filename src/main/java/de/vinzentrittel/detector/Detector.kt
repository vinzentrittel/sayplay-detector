package de.vinzentrittel.detector

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size

import de.vinzentrittel.detector.internal.extractRectangle
import de.vinzentrittel.detector.internal.extractParameters
import org.opencv.android.OpenCVLoader

fun initializeDetector() {
    OpenCVLoader.initLocal()
}

fun extractRectangle(source: Bitmap, width: Int, height: Int): Bitmap? {
    val extractedRectangle: Mat? = extractRectangle(
            Mat().apply {
                Utils.bitmapToMat(source, this)
            },
            Size(width.toDouble(), height.toDouble())
        )
    if (extractedRectangle == null) {
        return null
    }

    return createBitmap(width, height).apply {
        Utils.matToBitmap(extractedRectangle, this)
    }
}

fun extractParameters(source: Bitmap): List<Double> {
    return extractParameters(Mat().apply {
        Utils.bitmapToMat(source, this)
    })
}

fun extract(source: Bitmap, width: Int, height: Int): DetectorResult {
    return extractRectangle(source, width, height)?.let {
        DetectorResult(
            it,
            extractParameters(it)
        )
    }?: DetectorResult(source, listOf())
}