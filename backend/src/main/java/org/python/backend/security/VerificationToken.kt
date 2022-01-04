package org.python.backend.security

import java.nio.ByteBuffer

@Suppress("EqualsOrHashCode")
abstract class VerificationToken(@SecurityType val type: Int) {
    abstract class Builder {
        abstract fun build(): VerificationToken
    }

    abstract fun builder(): Builder

    /**
     * Verifies token correctness against some other object.
     * @param other Object to verify against. Could be (but does not have to be) another token.
     */
    abstract override fun equals(other: Any?): Boolean

    companion object {
        fun fromString(@SecurityType type: Int, string: String): VerificationToken {
            return when (type) {
                TYPE_PASS -> PasswordVerificationToken.fromString(string)
                else -> throw IllegalArgumentException("Unknown security type '$type' provided.")
            }
        }
    }
}

@Suppress("EqualsOrHashCode")
class PasswordVerificationToken(private val pass: ByteBuffer) : VerificationToken(TYPE_PASS) {
    /**
     * Password builder.
     * @note Updating values from an instance in multiple threads is explicitly not supported.
     */
    class PassBuilder : VerificationToken.Builder() {
        private var pass: ByteBuffer? = null

        fun with(password: ByteBuffer, salt: ByteArray?): PassBuilder {
            pass = Hasher.argon(password = password, salt = salt)
            return this
        }

        override fun build(): PasswordVerificationToken {
            synchronized(this) {
                if (pass != null)
                    return PasswordVerificationToken(pass = pass!!)
                throw IllegalStateException("Must set password when building PasswordVerificationToken")
            }
        }
    }

    override fun builder(): Builder = PassBuilder()

    override fun toString(): String = String(bytes = pass.array(), charset = Charsets.ISO_8859_1)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PasswordVerificationToken -> this.pass == other.pass
            is String -> this.pass == fromString(other).pass
            else -> throw java.lang.IllegalArgumentException("Cannot compare against $other")
        }
    }

    companion object {
        fun fromString(string: String): PasswordVerificationToken =
            PasswordVerificationToken(ByteBuffer.wrap(string.toByteArray(charset = Charsets.ISO_8859_1)))
    }
}
