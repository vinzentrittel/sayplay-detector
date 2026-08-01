package de.vinzentrittel.detector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.csvWriter
import com.jsoizo.kotlincsv.reader.readFromFile
import com.jsoizo.kotlincsv.writer.writeToFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Return type around results of `extract(Bitmap, Int, Int)`.
 * @see extract
 * @property[image] Result bitmap of `extract`. [image] will have dimensions as specified in
 * `extract`'s parameters. On failure to extract, [image] will contain the input image Bitmap.
 * @property[parameters] Result parameters, that describe [image] in Hu moments. On failure to
 * extract, [parameters] will be an empty List.
 * @property[success] True, if an image was extracted, otherwise false.
 */
data class DetectorResult(val image: Bitmap, val parameters: List<Double>) {
    val success: Boolean = parameters.isNotEmpty()

    /**
     * Store image data (PNG) and parameter (single line CSV) to the specified file destination.
     * A result that does not represent a successful extraction will not be saved as files.
     *
     * @param[fileFactoryFromExtension] an unary function to return a file from a given file
     * extension ('.csv', '.png').
     *
     * Usage:
     *
     * ```kotlin
     * val detectorResult: DetectorResult = ...
     * detectorResult.save { fileExtension ->
     *     File("myResult$fileExtension")
     * }
     * ```
     */
    fun save(fileFactoryFromExtension: (String) -> File) {
        if (!success)
            return

        FileOutputStream(fileFactoryFromExtension(".png")).use {
            image.compress(Bitmap.CompressFormat.PNG, 95, it)
        }
        csvWriter().run {
            writeToFile(
                sequenceOf(parameters.map {
                    it.toBigDecimal().toPlainString()
                }),
                fileFactoryFromExtension(".csv")
            )
        }
    }

    companion object {
        /**
         * Load image data (PNG) and parameter (single line CSV) from a specified file destination.
         * The files generated at location [fileFactoryFromExtension]('.png') and
         * [fileFactoryFromExtension]('.csv') must exist.
         *
         * @param[fileFactoryFromExtension] an unary function to return a file from a given file
         * extension ('.csv', '.png').
         * @throws DetectorLoadException.PngLoadFailed
         * @throws DetectorLoadException.CsvLoadFailed
         *
         * Usage:
         *
         * ```kotlin
         * val detectorResult: DetectorResult = detectorResult.load { fileExtension ->
         *     File("myResult$fileExtension")
         * }
         * ```
         */
        fun load(fileFactoryFromExtension: (String) -> File): DetectorResult {
            val bitmap = try {
                FileInputStream(fileFactoryFromExtension(".png")).use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                throw DetectorLoadException.PngLoadFailed(e)
            }

            val parameters: List<Double> = try {
                csvReader().run {
                    readFromFile(fileFactoryFromExtension(".csv")) { rows ->
                        rows.first().map { it.toDouble() }
                    }
                }
            } catch (e: Exception) {
                throw DetectorLoadException.CsvLoadFailed(e)
            }

            return DetectorResult(
                bitmap,
                parameters
            )
        }
    }
}