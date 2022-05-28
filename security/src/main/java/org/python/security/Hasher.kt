package org.python.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.ByteBuffer

data class Hash(val hash: ByteBuffer, val salt: ByteBuffer)

object Hasher {
    // https://github.com/lambdapioneer/argon2kt
    fun argon(password: ByteBuffer, salt: ByteBuffer? = null): Hash {
        val finalSalt = salt ?: ByteBuffer.allocateDirect(16).put(Securer.generateSalt(length = 16))
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