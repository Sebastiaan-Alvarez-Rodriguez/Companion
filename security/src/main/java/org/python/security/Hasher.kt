package org.python.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import org.python.security.util.SecurityUtil
import java.nio.ByteBuffer

data class Hash(val hash: ByteBuffer, val salt: ByteBuffer)

object Hasher {
    const val saltLength: Int = 16
    // https://github.com/lambdapioneer/argon2kt
    fun argon(password: ByteBuffer, salt: ByteBuffer? = null): Hash {
        val finalSalt = salt ?:
        SecurityUtil.bytesToBuf(Securer.generateSalt(length = saltLength))
        val argon2 = Argon2Kt()

        val hash = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = finalSalt,
            tCostInIterations = 5,
            mCostInKibibyte = 65536
        ).encodedOutput
        return Hash(hash, finalSalt)
    }
}