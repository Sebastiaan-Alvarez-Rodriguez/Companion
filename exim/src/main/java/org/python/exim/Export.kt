package org.python.exim

//import org.apache.avro.Schema
//import org.apache.avro.generic.GenericData
//import org.apache.parquet.avro.AvroParquetWriter
//import org.apache.parquet.hadoop.ParquetWriter
//import org.apache.parquet.hadoop.metadata.CompressionCodecName
//import org.apache.parquet.hadoop.util.HadoopOutputFile
import java.io.DataOutputStream

interface Exportable {
    suspend fun export(out: DataOutputStream)
}

// List of supported export functionality
//sealed class Exports {
//    data class parquet(val parquetConfig: Configuration, val schema: Schema) : Exports()
//}
//
//data class ParquetConfig(
//    val path: String,
//    /** parquet compression strategy */
//    val compression: Compression = Compression.SNAPPY,
//    /** parquet block size in bytes */
//    val blockSize: Long = ParquetWriter.DEFAULT_BLOCK_SIZE,
//    /** parquet page size in bytes */
//    val pageSize: Int = ParquetWriter.DEFAULT_PAGE_SIZE,
//)
//
//enum class Compression {
//    NONE,
//    SNAPPY
//}
//
//
//object Export {
//    suspend fun <T: Exportable> export(type: Exports, content: List<T>) {
//        when (type) {
//            is Exports.parquet -> writeToParquet(type.parquetConfig, type.schema, content)
//        }
//    }
//
//    private suspend fun <T: Exportable> writeToParquet(parquetConfig: ParquetConfig, schema: , content: List<T>) {
//        // TODO: Export to parquet
//        //TODO: Require hadoop core!
//        // TODO: Maybe keep hadoop-core's Configuration instead of our own?
//        val writer: ParquetWriter<GenericData.Record> = AvroParquetWriter.builder<GenericData.Record>(HadoopOutputFile.fromPath(parquetConfig.path))
//            .withSchema(schema)
//            .withCompressionCodec(
//                when(parquetConfig.compression) {
//                    Compression.SNAPPY -> CompressionCodecName.SNAPPY
//                    Compression.NONE -> CompressionCodecName.UNCOMPRESSED
//                    else -> throw IllegalAccessException("Could not find compression for value ${parquetConfig.compression}")
//                })
//            .withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
//            .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
//            .withConf(Configuration())
////            .withValidation(false)
////            .withDictionaryEncoding(false)
//            .build()
//        try {
//
//        } catch (e: Exception) {
//            writer.close()
//        }
////        StatefulBeanToCsvBuilder<T>(csvWriter)
////            .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
////            .build()
////            .write(content)
//    }
//}