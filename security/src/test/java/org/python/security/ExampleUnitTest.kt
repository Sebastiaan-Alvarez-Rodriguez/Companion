package org.python.security

import org.junit.Assert.assertEquals
import org.junit.Test
import org.python.security.util.SecurityUtil

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun salt_storage_reversible() {
        val saltArray = Securer.generateSalt(length = Hasher.saltLength)
        val saltBuf = SecurityUtil.bytesToBuf(saltArray)
        val saltString = SecurityUtil.bytesToString(saltArray)

        val verifySaltArray = SecurityUtil.stringToBytes(saltString)
        assert(saltArray.contentEquals(verifySaltArray))

        val verifySaltString = SecurityUtil.bytesToString(verifySaltArray)
        assertEquals(saltString, verifySaltString)

        val verifySaltBuf = SecurityUtil.stringToBuf(verifySaltString)
        assertEquals(saltBuf, verifySaltBuf)
    }
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}