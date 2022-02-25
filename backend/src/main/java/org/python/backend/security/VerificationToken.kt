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
                SecurityActor.TYPE_PASS -> PasswordVerificationToken.fromString(string)
                else -> throw IllegalArgumentException("Unknown security type '$type' provided.")
            }
        }
    }
}

class PasswordVerificationToken(private val pass: ByteBuffer) : VerificationToken(SecurityActor.TYPE_PASS) {
    /**
     * Password builder.
     * @note Updating values from an instance in multiple threads is explicitly not supported.
     */
    class PassBuilder : VerificationToken.Builder() {
        private var pass: ByteBuffer? = null

        fun with(password: ByteBuffer, salt: ByteBuffer? = null): PassBuilder {
            pass = Hasher.argon(
                password = if (password.isDirect) password else toDirectBuffer(password),
                salt = if (salt == null) null else if(salt.isDirect) salt else toDirectBuffer(salt),
            )
            return this
        }

        fun with(password: String, salt: String? = null): PassBuilder = with(toDirectBuffer(password), if (salt == null) null else toDirectBuffer(salt))

        override fun build(): PasswordVerificationToken {
            synchronized(this) {
                if (pass != null)
                    return PasswordVerificationToken(pass = pass!!)
                throw IllegalStateException("Must set password when building PasswordVerificationToken")
            }
        }

        @Suppress("unused")
        companion object {
            private fun toDirectBuffer(data: ByteBuffer): ByteBuffer = ByteBuffer.allocateDirect(data.capacity()).put(data)
            private fun toDirectBuffer(data: ByteArray): ByteBuffer = ByteBuffer.allocateDirect(data.size).put(data)
            private fun toDirectBuffer(data: String, toISO_8859_1: Boolean = true): ByteBuffer =
                ByteBuffer.allocateDirect(data.length).put(if (toISO_8859_1) data.toByteArray(charset = Charsets.ISO_8859_1) else data.toByteArray())
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

    override fun hashCode(): Int {
        return pass.hashCode()
    }

    companion object {
        fun fromString(string: String): PasswordVerificationToken =
            PasswordVerificationToken(ByteBuffer.wrap(string.toByteArray(charset = Charsets.ISO_8859_1)))
    }
}
