package org.python.exim

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ZipInstrumentedTest {
    private fun prepareFile(nameIn: String): File {
        val tmpDir = Files.createTempDirectory("companion")
        return File(tmpDir.toFile(), nameIn)
    }

    private fun prepareDir(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.cacheDir
    }

    private fun prepareOurZip(fileIn: File, nameIn: String = "notes.pq", password: String = "password"): File {
        var progress = 0f
        val passwordArray = password.toCharArray()

        val fileOut = prepareFile(nameIn)
        val fileOutPath = fileOut.path
        fileOut.delete()

        runBlocking {
            Export.zip(
                inputs = listOf(fileIn),
                password = passwordArray,
                destination = fileOutPath,
                pollTimeMS = 100L,
                onProgress = { p: Float ->
                    progress = p
                }
            )
        }
        Assert.assertEquals(1f, progress)
        return fileOut
    }

    @Test
    fun basicZipWorks() {
        val nameIn = "test.in"
        val fileIn = prepareFile(nameIn)
        fileIn.writeText("This is a test. Do not be alarmed.")
        val fileOut = prepareFile(nameIn)
        val fileOutPath = fileOut.path
        fileOut.delete()

        val zipFile = ZipFile(fileOutPath)
        zipFile.use { zip ->
            zip.isRunInThread = false
            zip.addFile(fileIn)
        }
        assert(zipFile.isValidZipFile)

        val verificationZipFile = ZipFile(fileOutPath)
        assert(verificationZipFile.isValidZipFile)
    }

    @Test
    fun ourZipWorks() {
        val nameIn = "test.in"
        val fileIn = prepareFile(nameIn)
        fileIn.writeText("This is a test. Do not be alarmed.")

        val zipFile = prepareOurZip(fileIn)

        assert(ZipFile(zipFile).isValidZipFile)
        Assert.assertEquals(1, ZipFile(zipFile).fileHeaders.size)
        assert(ZipFile(zipFile.path).isValidZipFile)
    }

    @Test
    fun ourUnzipWorks() {
        var progress = 0f
        val nameIn = "notes.pq"
        val password = "password"
        val fileIn = prepareFile(nameIn)
        fileIn.writeText("This is a test. Do not be alarmed.")
        val dirOut = prepareDir()

        val zipFile = prepareOurZip(fileIn, nameIn = nameIn, password = password)

        runBlocking {
            Import.unzip(
                input = zipFile,
                password = password.toCharArray(),
                destination = dirOut.path,
                pollTimeMS = 100
            ) {
                progress = it
            }
        }
        Assert.assertEquals(1f, progress)
        Assert.assertEquals(1, ZipFile(zipFile).fileHeaders.size)
        assert(dirOut.resolve(nameIn).isFile)
    }
}