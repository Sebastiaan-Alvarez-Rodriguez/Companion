package org.python.security.util

import java.nio.ByteBuffer

object SecurityUtil {
    fun stringToBytes(s: String) = s.toByteArray(Charsets.ISO_8859_1)
    fun bytesToBuf(b: ByteArray): ByteBuffer = ByteBuffer.allocateDirect(b.size).put(b)

    fun bufToDirectBuf(b: ByteBuffer): ByteBuffer = if (b.isDirect) b else ByteBuffer.allocateDirect(b.capacity()).put(b)

    fun stringToBuf(s: String): ByteBuffer = bytesToBuf(stringToBytes(s))

    fun bufToBytes(b: ByteBuffer): ByteArray = ByteArray(b.capacity(), init = { idx -> b.array()[idx+b.arrayOffset()] })
    fun bytesToString(b: ByteArray) = String(b, Charsets.ISO_8859_1)
    fun bufToString(b: ByteBuffer) = bytesToString(bufToBytes(b))
}