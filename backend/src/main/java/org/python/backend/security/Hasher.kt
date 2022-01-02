package org.python.backend.security

// https://github.com/lambdapioneer/argon2kt
@Throws(Argon2Exception::class)
private fun hash(password: ByteArray): String {
    val salt = byteArrayOf(
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
    val argon2: Argon2 = Builder(Version.V13)
        .type(Argon2id)
        .memoryCost(MemoryCost.MiB(32))
        .parallelism(1)
        .iterations(3)
        .hashLength(32)
        .build()
    val result: Argon2.Result = argon2.hash(password, salt)
    return result.getEncoded()
}