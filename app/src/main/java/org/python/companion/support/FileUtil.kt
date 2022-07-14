package org.python.companion.support

import android.content.res.AssetFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

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
            var count: Int
            var prog = 0L
            try {
                do {
                    count = inStream.read(bytes)
                    if (count > 0) {
                        outStream.write(bytes, 0, count)
                        prog += count
                        onProgress(prog.toFloat() / size)
                    }
                } while (count > 0)
            } finally {
                outStream.flush()
                outStream.close()
                inStream.close()
            }
        }
    }

    fun determineSize(fd: AssetFileDescriptor): Long = fd.use { x -> return x.length }

    suspend fun deleteDirectory(path: Path?) = withContext(Dispatchers.IO) {
        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete)
    }

    suspend fun compareByMemoryMappedFiles(path1: Path, path2: Path): Boolean = withContext(Dispatchers.IO) {
        RandomAccessFile(path1.toFile(), "r").use { randomAccessFile1 ->
            RandomAccessFile(path2.toFile(), "r").use { randomAccessFile2 ->
                val ch1: FileChannel = randomAccessFile1.channel
                val ch2: FileChannel = randomAccessFile2.channel
                if (ch1.size() != ch2.size())
                    return@use false
                val size: Long = ch1.size()
                val m1: MappedByteBuffer = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size)
                val m2: MappedByteBuffer = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size)
                return@use m1 == m2
            }
        }
    }
}