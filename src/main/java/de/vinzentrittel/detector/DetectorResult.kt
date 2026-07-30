package de.vinzentrittel.detector

import android.graphics.Bitmap

data class DetectorResult(val image: Bitmap, val parameters: List<Double>) {
    val success: Boolean = !parameters.isEmpty()
}