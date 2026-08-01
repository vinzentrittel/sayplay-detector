@file:Suppress("FunctionName")

package de.vinzentrittel.detector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DetectorResultTest {
    private lateinit var bitmap: Bitmap

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class, BitmapFactory::class)
        bitmap = mockk(relaxed = true)
        every { bitmap.width } returns 512
        every { bitmap.height } returns 512

        every { BitmapFactory.decodeStream(any()) } returns bitmap
        // Mocking the enum value access if needed, though MockK usually handles it
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `success is true when parameters are not empty`() {
        val bitmap = mockk<Bitmap>()
        val result = DetectorResult(bitmap, listOf(1.0, 2.0))
        assertTrue(result.success)
    }

    @Test
    fun `success is false when parameters are empty`() {
        val bitmap = mockk<Bitmap>()
        val result = DetectorResult(bitmap, emptyList())
        assertFalse(result.success)
    }

    @Test
    fun `save creates files with correct extensions and calls compress`() {
        val bitmap = mockk<Bitmap>()
        every { bitmap.compress(any(), any(), any()) } returns true

        val result = DetectorResult(bitmap, listOf(1.23, 4.56))

        val createdExtensions = mutableListOf<String>()
        val fileFactory: (String) -> File = { ext ->
            createdExtensions.add(ext)
            tempFolder.newFile("test${System.currentTimeMillis()}$ext")
        }

        result.save(fileFactory)

        assertTrue("Should save PNG", createdExtensions.contains(".png"))
        assertTrue("Should save CSV", createdExtensions.contains(".csv"))
        verify { bitmap.compress(Bitmap.CompressFormat.PNG, 95, any()) }
    }

    @Test
    fun `save writes correct parameters to CSV`() {
        val bitmap = mockk<Bitmap>()
        every { bitmap.compress(any(), any(), any()) } returns true

        val parameters = listOf(1.0, 2.5, 3.14159)
        val result = DetectorResult(bitmap, parameters)

        val csvFile = tempFolder.newFile("save_test.csv")
        val pngFile = tempFolder.newFile("save_test.png")

        result.save { ext -> if (ext == ".csv") csvFile else pngFile }

        val lines = csvFile.readLines()
        assertTrue(lines.isNotEmpty())
        assertEquals("1.0,2.5,3.14159", lines[0])
    }

    @Test
    fun `load loads correct parameters from CSV`() {
        val csvFile = tempFolder.newFile("load_test.csv")
        val pngFile = tempFolder.newFile("load_test.png")

        csvFile.writer().run {
            write("1.0,2.5,3.14159")
            close()
        }

        DetectorResult.load { ext ->
            if (ext == ".csv") csvFile else pngFile
        }.run {
            assertTrue(success)
        }
    }

    @Test(expected = DetectorLoadException.PngLoadFailed::class)
    fun `load loads from missing PNG file`() {
        val csvFile = tempFolder.newFile("load_test.csv")
        csvFile.writer().run {
            write("1.0,2.5,3.14159")
        }

        DetectorResult.load { ext ->
            File(tempFolder.root.path, "load_test$ext")
        }
    }

    @Test(expected = DetectorLoadException.CsvLoadFailed::class)
    fun `load loads from missing CSV file`() {
        val pngFile = tempFolder.newFile("load_test.png")

        DetectorResult.load { ext ->
            File(tempFolder.root.path, "load_test$ext")
        }
    }
}
