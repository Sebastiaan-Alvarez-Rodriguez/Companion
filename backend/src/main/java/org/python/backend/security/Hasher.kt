package org.python.backend.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.ByteBuffer

private val DEFAULT_SALT = byteArrayOf(
    0x42,
    0x00,
    0x77,
    0x52,
    0x36,
    0x4F,
    0x55,
    0x13,
    0x51,
    0x33,
    0x24,
    0x45,
    0x31,
    0x10,
    0x04,
    0x39
)

object Hasher {
    // https://github.com/lambdapioneer/argon2kt
    fun argon(password: ByteBuffer, salt: ByteArray? = null): ByteBuffer {
        val finalSalt = salt ?: DEFAULT_SALT
        val argon2 = Argon2Kt()

        return argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = ByteBuffer.wrap(finalSalt),
            tCostInIterations = 5,
            mCostInKibibyte = 65536
        ).encodedOutput
    }
}