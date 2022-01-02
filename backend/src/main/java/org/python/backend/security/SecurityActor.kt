package org.python.backend.security

import android.content.SharedPreferences
import java.lang.IllegalArgumentException

abstract class SecurityActor(
    protected val sharedPreferences: SharedPreferences,
    @SecurityType protected val type: Int
) {
    /** Whether a credential has been set or not. */
    abstract fun hasCredentials(): Boolean

    /** Setup a credential.
     * If a credential exists, this function calls <code>verify()</code>.
     * In such cases, caller must pass a verification object.
     * @param oldToken Optional object to call with verify-call. Required if a credential exists.
     * @param newToken Credentials to set.
     * @return status object. If 'correct', credentials were set/updated. Failure indication otherwise.
     */
    abstract fun setCredentials(oldToken: VerificationToken? = null, newToken: VerificationToken): VerificationMessage

    /**
     * Verifies given token for correctness.
     * @param token Token to verify.
     * @return message indicating verification result status.
     */
    abstract fun verify(token: VerificationToken): VerificationMessage
}


class PassActor(sharedPreferences: SharedPreferences) : SecurityActor(sharedPreferences, TYPE_PASS) {
    private companion object{
        const val pass_storage_key = "SECURITY_PASS"
    }
    @Synchronized override fun hasCredentials(): Boolean {
        return sharedPreferences.contains(pass_storage_key)
    }

    @Synchronized override fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): VerificationMessage {
        if (hasCredentials()) {
            val result = when (oldToken) {
                null -> throw IllegalArgumentException("Existing credentials detected. Caller must provide old verification token for verification.")
                else -> verify(oldToken)
            }
            if (result.type != SEC_CORRECT)
                return result
        }

        val preferencesEditor = sharedPreferences.edit()
        preferencesEditor.putString(pass_storage_key, newToken.toString())
        preferencesEditor.apply()

        return VerificationMessage.createCorrect()
    }

    @Synchronized override fun verify(token: VerificationToken): VerificationMessage {
        return 
    }
}