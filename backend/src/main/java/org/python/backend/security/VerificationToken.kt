package org.python.backend.security

abstract class VerificationToken(@SecurityType type: Int) {

    companion object {
        fun fromString(@SecurityType type: Int, string: String): VerificationToken {
            return when (type) {
                TYPE_PASS -> PasswordVerificationToken.fromString(string)
                else -> throw IllegalArgumentException("Unknown security type '$type' provided.")
            }
        }
    }
}

class PasswordVerificationToken(private val pass: ByteArray) : VerificationToken(TYPE_PASS) {
    override fun toString(): String = String(bytes = pass, charset = Charsets.ISO_8859_1)

    companion object {
        fun fromString(string: String): PasswordVerificationToken =
            PasswordVerificationToken(string.toByteArray(charset = Charsets.ISO_8859_1))
    }
}
