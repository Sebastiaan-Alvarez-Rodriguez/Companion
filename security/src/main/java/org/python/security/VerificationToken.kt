package org.python.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import org.python.security.util.SecurityUtil
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
                password = SecurityUtil.bufToDirectBuf(password),
                salt = if (salt == null) null else SecurityUtil.bufToDirectBuf(salt),
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
            with(SecurityUtil.stringToBuf(password), if (salt == null) null else SecurityUtil.stringToBuf(salt))

        fun with(password: ByteBuffer, storedSaltContext: Activity): PassBuilder =
            getSalt(storedSaltContext)?.let { with(password, SecurityUtil.stringToBuf(it))} ?: with(password)
        fun with(password: String, storedSaltContext: Activity): PassBuilder =
            with(SecurityUtil.stringToBuf(password), storedSaltContext)

        fun with(password: ByteBuffer, storedSaltContext: Context): PassBuilder =
            getSalt(storedSaltContext)?.let { with(password, SecurityUtil.stringToBuf(it)) } ?: with(password)
        fun with(password: String, storedSaltContext: Context): PassBuilder =
            with(SecurityUtil.stringToBuf(password), storedSaltContext)

        fun with(password: ByteBuffer, storedSaltContext: SharedPreferences): PassBuilder =
            getSalt(storedSaltContext)?.let { with(password, SecurityUtil.stringToBuf(it)) } ?: with(password)
        fun with(password: String, storedSaltContext: SharedPreferences): PassBuilder =
            with(SecurityUtil.stringToBuf(password), storedSaltContext)

        override fun build(): PasswordVerificationToken = synchronized(this) {
            hash?.let {
                return PasswordVerificationToken(hashedObject = it)
            }
            throw IllegalStateException("Must set password & salt when building PasswordVerificationToken")
        }
    }

    override fun builder(): Builder = PassBuilder()

    override fun toString(): String = hashString()

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is PasswordVerificationToken -> hashedObject.hash.array().contentEquals(other.hashedObject.hash.array())
            is Hash -> hashedObject.hash.array().contentEquals(other.hash.array())
            else -> throw java.lang.IllegalArgumentException("Cannot compare against $other")
        }
    }

    fun hashString(): String = SecurityUtil.bufToString(hashedObject.hash)
    fun saltString(): String = SecurityUtil.bufToString(hashedObject.salt)

    override fun hashCode(): Int = hashedObject.hash.hashCode()

    companion object {
        private fun getSalt(activity: Activity): String? = getSalt(activity.baseContext)
        private fun getSalt(context: Context): String? = getSalt(context.getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE))
        private fun getSalt(sharedPreferences: SharedPreferences): String? =
            sharedPreferences.getString(PassActor.salt_storage_key, null)

        fun fromString(hash: String, salt: String): PasswordVerificationToken = PasswordVerificationToken(
            hashedObject = Hash(
                hash = SecurityUtil.stringToBuf(hash),
                salt = SecurityUtil.stringToBuf(salt)
            )
        )
    }
}
