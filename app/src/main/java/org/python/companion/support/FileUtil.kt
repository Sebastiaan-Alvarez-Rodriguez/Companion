package org.python.companion.support

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

object FileUtil {
    /**
     * Copies input to output stream, publishing progress on the way.
     * @param size input stream data size coming in.
     * @param inStream input stream.
     * @param outStream output stream.
     * @param onProgress lambda progress function. Progress ranges from 0f to 1f.
     * @return job for copying.
     */
    suspend fun copyStream(
        size: Long,
        inStream: InputStream,
        outStream: OutputStream,
        onProgress: (Float) -> Unit
    ): Job = withContext(Dispatchers.IO) {
        return@withContext launch {
            val bufferSize = 4096
            val bytes = ByteArray(bufferSize)
            var count = 0
            var prog = 0L
            try {
                while (count != -1) {
                    count = inStream.read(bytes)
                    if (count != -1) {
                        outStream.write(bytes, 0, count)
                        prog += count
                        onProgress(prog.toFloat() / size)
                    }
                }
            } finally {
                outStream.flush()
                inStream.close()
                outStream.close()
            }
        }
    }
}