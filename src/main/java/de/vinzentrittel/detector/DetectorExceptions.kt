package de.vinzentrittel.detector

sealed class DetectorLoadException(message: String, cause: Throwable? = null)
    : Exception(message, cause) {
    class PngLoadFailed(cause: Throwable) : DetectorLoadException("Failed to load PNG", cause)
    class CsvLoadFailed(cause: Throwable)
        : DetectorLoadException("Failed to load parameter CSV file", cause)
}