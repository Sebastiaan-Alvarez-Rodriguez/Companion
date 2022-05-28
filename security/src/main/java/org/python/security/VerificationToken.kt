package org.python.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
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
}

class PasswordVerificationToken(private val hashedObject: Hash) : VerificationToken(SecurityActor.TYPE_PASS) {
    /**
     * Password builder.
     * @note Updating values from an instance in multiple threads is explicitly not supported.
     */
    class PassBuilder : Builder() {
        private var hash: Hash? = null

        private fun with(password: ByteBuffer, salt: ByteBuffer? = null): PassBuilder {
            hash = Hasher.argon(
                password = if (password.isDirect) password else toDirectBuffer(password),
                salt = if (salt == null) null else if(salt.isDirect) salt else toDirectBuffer(salt),
            )
            return this
        }

        /**
         * Sets password hash.
         * @param password Password to hash
         * @param salt Salt to use. If null, creates secure random salt.
         * @return this
         */
        fun with(password: String, salt: String? = null): PassBuilder =
            with(toDirectBuffer(password), if (salt == null) null else toDirectBuffer(salt))

        fun with(password: ByteBuffer, storedSaltContext: Activity): PassBuilder =
            getSalt(storedSaltContext)?.let { with(password, toDirectBuffer(it))} ?: with(password)
        fun with(password: String, storedSaltContext: Activity): PassBuilder =
            with(toDirectBuffer(password), storedSaltContext)

        fun with(password: ByteBuffer, storedSaltContext: Context): PassBuilder =
            getSalt(storedSaltContext)?.let { with(password, toDirectBuffer(it))} ?: with(password)
        fun with(password: String, storedSaltContext: Context): PassBuilder =
            with(toDirectBuffer(password), storedSaltContext)

        fun with(password: ByteBuffer, storedSaltContext: SharedPreferences): PassBuilder =
            getSalt(storedSaltContext)?.let { with(password, toDirectBuffer(it))} ?: with(password)
        fun with(password: String, storedSaltContext: SharedPreferences): PassBuilder =
            with(toDirectBuffer(password), storedSaltContext)

        override fun build(): PasswordVerificationToken = synchronized(this) {
            hash?.let {
                return PasswordVerificationToken(hashedObject = it)
            }
            throw IllegalStateException("Must set password & salt when building PasswordVerificationToken")
        }

        @Suppress("unused")
        companion object {
            private fun toDirectBuffer(data: ByteBuffer): ByteBuffer = ByteBuffer.allocateDirect(data.capacity()).put(data)
            private fun toDirectBuffer(data: ByteArray): ByteBuffer = ByteBuffer.allocateDirect(data.size).put(data)
            private fun toDirectBuffer(data: String, toISO_8859_1: Boolean = true): ByteBuffer =
                toDirectBuffer(if (toISO_8859_1) data.toByteArray(charset = Charsets.ISO_8859_1) else data.toByteArray())

            private fun getSalt(activity: Activity): String? = getSalt(activity.baseContext)
            private fun getSalt(context: Context): String? = getSalt(context.getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE))
            private fun getSalt(sharedPreferences: SharedPreferences): String? = sharedPreferences.getString(PassActor.salt_storage_key, null)
        }
    }

    override fun builder(): Builder = PassBuilder()

    override fun toString(): String = hashString()

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PasswordVerificationToken -> {
                val tmp = hashedObject.hash.array().contentEquals(other.hashedObject.hash.array())
                tmp
            }
            is Hash -> hashedObject.hash == other.hash
            else -> throw java.lang.IllegalArgumentException("Cannot compare against $other")
        }
    }

    fun hashString(): String = String(bytes = hashedObject.hash.array(), charset = Charsets.ISO_8859_1)
    fun saltString(): String = String(bytes = hashedObject.salt.array(), charset = Charsets.ISO_8859_1)

    override fun hashCode(): Int = hashedObject.hash.hashCode()

    companion object {
        fun fromString(hash: String, salt: String): PasswordVerificationToken = PasswordVerificationToken(
            hashedObject = Hash(
                hash = ByteBuffer.wrap(hash.toByteArray(charset = Charsets.ISO_8859_1)),
                salt = ByteBuffer.wrap(salt.toByteArray(charset = Charsets.ISO_8859_1))
            )
        )
    }
}
