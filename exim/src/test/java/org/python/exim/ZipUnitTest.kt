package org.python.exim

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/** Testsuite for zips */
class ZipUnitTest {
    @TempDir
    lateinit var tempDir: Path

    private suspend fun prepareOurZip(
        inputs: List<File>,
        outputPath: Path,
        password: String = "password",
        onProgress: (Float) -> Unit = {}
    ): Deferred<EximUtil.ZippingState> {
        val passwordArray = password.toCharArray()
        return Export.zip(
            inputs = inputs,
            password = passwordArray,
            destination = outputPath,
            pollTimeMS = 100L,
            onProgress = onProgress
        )
    }

    private suspend fun prepareOurUnzip(
        inputPath: Path,
        outputPath: Path,
        password: String = "password",
        onProgress: (Float) -> Unit
    ): Deferred<EximUtil.ZippingState> {
        val passwordArray = password.toCharArray()
        return Import.unzip(
            input = inputPath,
            password = passwordArray,
            destination = outputPath,
            pollTimeMS = 100L,
            onProgress = onProgress
        )
    }

    @Test
    fun basicZipWorks() {
        val pathIn = tempDir.resolve("basicZipWorks.in")
        val pathOut = tempDir.resolve("basicZipWorks.out")

        val fileIn = pathIn.toFile()
        fileIn.writeText("This is a test. Do not be alarmed.")

        val zipFile = ZipFile(pathOut.toFile())
        zipFile.use { zip ->
            zip.isRunInThread = false
            zip.addFile(fileIn)
        }
        assert(zipFile.isValidZipFile)
        assert(ZipFile(pathOut.toString()).isValidZipFile)
    }

    @Test
    fun ourZipWorks() {
        val pathIn = tempDir.resolve("ourZipWorks.in")
        val pathOut = tempDir.resolve("ourZipWorks.out")

        val fileIn = pathIn.toFile()
        fileIn.writeText("This is a test. Do not be alarmed.")

        var zipProgress = 0f
        val zippingState = runBlocking {
            prepareOurZip(
                inputs = listOf(fileIn),
                outputPath = pathOut,
                onProgress = { progress -> zipProgress = progress }
            ).await()
        }
        Assertions.assertEquals(EximUtil.FinishState.SUCCESS ,zippingState.state)
        Assertions.assertEquals(1f, zipProgress)
        val verificationZipFile = ZipFile(pathOut.toString())
        Assertions.assertTrue(verificationZipFile.isValidZipFile)
        Assertions.assertEquals(1, verificationZipFile.fileHeaders.size)
        Assertions.assertTrue(verificationZipFile.isValidZipFile)
    }

    @Test
    fun ourUnzipWorks() {
        val testText = "This is a test. Do not be alarmed."

        val pathIn = tempDir.resolve("ourUnzipWorks.in")
        val pathZip = tempDir.resolve("ourUnzipWorks.zip")
        val pathOut = tempDir.resolve("ourUnzipWorks_outdir")
        val pathFileOut = pathOut.resolve("ourUnzipWorks.in")

        pathOut.createDirectory()

        val fileIn = pathIn.toFile()
        fileIn.writeText(testText)

        runBlocking {
            prepareOurZip(
                inputs = listOf(fileIn),
                outputPath = pathZip,
            ).await()
        }

        var unzipProgress = 0f
        val unzippingState = runBlocking {
            prepareOurUnzip(
                inputPath = pathZip,
                outputPath = pathOut,
                password = "password",
                onProgress = { progress -> unzipProgress = progress }
            ).await()
        }
        Assertions.assertEquals(EximUtil.FinishState.SUCCESS ,unzippingState.state)
        Assertions.assertEquals(1f, unzipProgress)

        Assertions.assertTrue(pathFileOut.isRegularFile())
        Assertions.assertEquals(testText, pathFileOut.readText())
    }
}