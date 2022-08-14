package org.python.exim

import blue.strategic.parquet.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Type
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

/**
 * Example local unit test, which will execute on the development machine (host).
 */
internal class ParquetInstrumentedTest {
    companion object {
        const val numRows = 1000
        val data: List<Row> =
            (0 until numRows)
                .map { i -> (Row(i.toLong(), "KingHenryThe$i", 18 + i % 10)) }
                .toList()

        /** Currently supported codecs (with the required dependency behind them)  */
        val SUPPORTED_COMPRESSION_CODECS = arrayOf(
            CompressionCodecName.UNCOMPRESSED,  // no dependency needed
            CompressionCodecName.SNAPPY // implementation "com.github.Sebastiaan-Alvarez-Rodriguez:snappy-android:1.1.9"
        )

        val rowDehydrator: Dehydrator<Row> = Dehydrator<Row> { record, valueWriter ->
                for (field in record.values()) {
                    valueWriter.write(field.name, field.value)
                }
            }
        val rowHydrator: Hydrator<MutableList<Any>, Row> = object : Hydrator<MutableList<Any>, Row> {
                override fun start(): MutableList<Any> {
                    return ArrayList<Any>(Importable.amountValues(Row::class.java))
                }

                override fun add(target: MutableList<Any>, heading: String, value: Any): MutableList<Any> {
                    target.add(value)
                    return target
                }

                override fun finish(target: MutableList<Any>): Row {
                    return Importable.fromValues(target, Row::class.java)
                }
            }

    }

    @TempDir
    var tempDir: Path? = null


    private suspend fun prepareExport(outPath: Path, onProgress: (Row, Long) -> Unit = {_, _ ->}): Job {
        val types: List<Type> = data.first().values().map { item -> Exports.parquet.transform(item.value, item.name) }
        return Export.export(
            type = Exports.parquet(schema = MessageType("testSchema", types)),
            destination = outPath.toFile(),
            content = data,
            onProgress = onProgress
        )
    }
    @ParameterizedTest
    @EnumSource(CompressionCodecName::class)
    fun writeParquet(compressionCodecName: CompressionCodecName) {
        if (Arrays.stream(SUPPORTED_COMPRESSION_CODECS).noneMatch { codec: CompressionCodecName -> codec == compressionCodecName })
            return  // We do not support this codec yet
        val outPath = tempDir!!.resolve("writeParquet.parquet")

        var writtenRows = 0L
        runBlocking {
            prepareExport(outPath) { item, amountProcessed ->
                writtenRows = amountProcessed
            }.join()
        }

        Assertions.assertEquals(numRows.toLong(), writtenRows)
        ParquetAssertHelper.assertWritten<Row>(data, outPath, HydratorSupplier.constantly(rowHydrator))
    }

    @ParameterizedTest
    @EnumSource(CompressionCodecName::class)
    fun readParquet(compressionCodecName: CompressionCodecName) {
        if (Arrays.stream(SUPPORTED_COMPRESSION_CODECS).noneMatch { codec: CompressionCodecName -> codec == compressionCodecName })
            return  // We do not support this codec yet
        val outPath = tempDir!!.resolve("readParquet.parquet")

        runBlocking { prepareExport(outPath).join() }
        ParquetAssertHelper.assertWritten(data, outPath, HydratorSupplier.constantly(rowHydrator))
        ParquetReader.streamContent(outPath.toFile(), HydratorSupplier.constantly(rowHydrator))
            .use { readStream ->
                val readData: List<Row> = readStream.collect(Collectors.toList())
                Assertions.assertEquals(data, readData)
            }
    }
}