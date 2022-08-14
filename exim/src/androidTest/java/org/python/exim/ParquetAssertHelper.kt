package org.python.exim

import blue.strategic.parquet.HydratorSupplier
import blue.strategic.parquet.ParquetReader
import org.junit.jupiter.api.Assertions
import java.io.File
import java.nio.file.Path
import java.util.stream.BaseStream

object ParquetAssertHelper {
    fun <T> assertWritten(data: List<T>, file: Path, supplier: HydratorSupplier<MutableList<Any>, T>) {
        assertWritten(data, file.toFile(), supplier)
    }

    /** Asserts that given data is written to given file  */
    fun <T> assertWritten(data: List<T>, file: File?, supplier: HydratorSupplier<MutableList<Any>, T>) {
        val readStream = ParquetReader.streamContent(file, supplier)
        assertStreamEquals(data.stream(), readStream)
    }

    /** Asserts that 2 sequential, ordered streams are equivalent. Closes the streams.  */
    fun assertStreamEquals(expected: BaseStream<*, *>, actual: BaseStream<*, *>) {
        expected.use {
            actual.use {
                val e = expected.iterator()
                val a = actual.iterator()
                while (e.hasNext() && a.hasNext()) {
                    Assertions.assertEquals(e.next(), a.next())
                }
                Assertions.assertFalse(e.hasNext())
                Assertions.assertFalse(a.hasNext())
            }
        }
    }
}