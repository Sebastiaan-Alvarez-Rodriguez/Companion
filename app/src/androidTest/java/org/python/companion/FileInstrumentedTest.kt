package org.python.companion

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.python.companion.support.FileUtil
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class FileInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.python.companion", appContext.packageName)
    }

    @Test
    fun copyWorks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cacheDir = context.cacheDir
        val contentResolver = context.contentResolver
        val textIn = (1..100).asSequence().map { "This is a test. Do not be alarmed." }.joinToString(separator = "\n")
        var progress = 0f

        val fileIn = File.createTempFile("test", "in", cacheDir)
        val fileOut = File.createTempFile("test", "out", cacheDir)

        fileIn.writeText(textIn)

        runBlocking {
            val copyJob = FileUtil.copyStream(
                size = fileIn.length(),
                inStream = fileIn.inputStream(),
                outStream = contentResolver.openOutputStream(fileOut.toUri(), "w")!!
            ) { progress = it }
            copyJob.start()
            copyJob.join()
            assert(FileUtil.compareByMemoryMappedFiles(fileIn.toPath(), fileOut.toPath()))
            assertEquals(1f, progress)
            assertEquals(textIn, fileOut.readText())
        }
    }
}