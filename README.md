# Detector

> A small Android/Kotlin library that finds a rectangular card in a photo, straightens it out, and describes its shape.

`detector` takes a `Bitmap`, hunts for the most prominent rectangle inside it (think: a playing card on a table), warps that rectangle into a clean, flat image of the size you ask for, and computes a compact shape signature ([Hu moments](https://en.wikipedia.org/wiki/Image_moment#Rotation_invariants)) you can use to compare cards. Under the hood it leans on [OpenCV](https://opencv.org/) for the heavy image processing.

Part of the **SayPlay / cardharvest** project.

---

## What it does

- **Detects** the largest rectangle in an image using edge and line analysis.
- **Extracts & rectifies** that rectangle into a bitmap of your chosen dimensions.
- **Describes** the result with rotation-invariant shape parameters (Hu moments).
- **Persists** results to disk as a `.png` + single-line `.csv` pair, and loads them back.

## Installation

The library is published to GitHub Packages as a Maven artifact:

```kotlin
// build.gradle.kts
dependencies {
    implementation("de.vinzentrittel.sayplay:detector:0.0.5")
}
```

Point Gradle at the package repository:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/vinzentrittel/sayplay-detector")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

**Requirements:** Android `minSdk 26`, `compileSdk 35`, Java 11.

## Usage

Initialize OpenCV once before you use the detector (e.g. at app startup):

```kotlin
initializeDetector()
```

Then extract a card from a source bitmap:

```kotlin
val result: DetectorResult = extract(sourceBitmap, width = 640, height = 400)

if (result.success) {
    val card: Bitmap = result.image          // the straightened rectangle
    val shape: List<Double> = result.parameters  // Hu-moment shape signature
}
```

On failure, `success` is `false`, `image` holds the original input, and `parameters` is empty.

### Saving & loading results

`DetectorResult` can serialize itself to a `.png` (image) and `.csv` (parameters) pair. You supply a factory that maps a file extension to a `File`:

```kotlin
// Save
result.save { extension -> File(cacheDir, "myCard$extension") }

// Load again later
val restored = DetectorResult.load { extension -> File(cacheDir, "myCard$extension") }
```

Loading throws `DetectorLoadException.PngLoadFailed` or `DetectorLoadException.CsvLoadFailed` if the files are missing or corrupt. Unsuccessful results are never written to disk.

## Building from source

```bash
./gradlew build        # compile the library
./gradlew test         # run the unit tests
./gradlew dokkaHtml    # generate API documentation
```

---

## License & Attribution

This product includes software developed by the **OpenCV project**.

> NOTICE:
> This product includes software developed by the OpenCV project.
> Copyright (C) 2000-2024, Intel Corporation, all rights reserved.
> ...
> Licensed under the Apache License, Version 2.0.
