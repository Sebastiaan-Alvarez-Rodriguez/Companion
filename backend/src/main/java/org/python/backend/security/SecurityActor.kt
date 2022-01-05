package org.python.backend.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import timber.log.Timber

abstract class SecurityActor(@SecurityType protected val type: Int) {
    companion object {
        const val security_storage = "SECURITY_ACTOR_STORAGE"
    }

    /**
     * Whether this actor is available at this time.
     * @return Message with <code>message.type == SEC_CORRECT</code> and no body on success,
     * message with a human-readable error otherwise.
     */
    abstract fun actorAvailable(): VerificationMessage

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

class BioActor(protected val context: Context) : SecurityActor(TYPE_BIO) {
    constructor(activity: Activity) : this(activity.baseContext)

    override fun actorAvailable(): VerificationMessage {
        val biometricManager = BiometricManager.from(context)
        val status = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> VerificationMessage.createCorrect()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> VerificationMessage.createIncorrect(
                body = StatusBody.IncorrectBody("Biometrics hardware in use by other app.")
            )
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> VerificationMessage.createIncorrect(
                body = StatusBody.IncorrectBody("No biometrics enrolled")
            )
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> VerificationMessage.createIncorrect(
                body = StatusBody.IncorrectBody("No biometrics hardware detected")
            )
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> VerificationMessage.createIncorrect(
                body = StatusBody.IncorrectBody("Biometrics hardware does not satisfy this app's requirements")
            )
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> VerificationMessage.createIncorrect(
                body = StatusBody.IncorrectBody("Outdated software for biometrics hardware detected. Update required.")
            )
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> VerificationMessage.createIncorrect(
                body = StatusBody.IncorrectBody("Unknown error occurred. Please try again.")
            )
            else -> {
                Timber.w("actorAvailable: BioActor: Received unknown biometrics status code '$status'")
                VerificationMessage.createIncorrect(
                    body = StatusBody.IncorrectBody("Unknown error occurred. Please try again.")
                )
            }
        }
    }

    override fun hasCredentials(): Boolean = actorAvailable().type == SEC_CORRECT

    override fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): VerificationMessage = VerificationMessage.createCorrect()

    override fun verify(token: VerificationToken): VerificationMessage {
        // https://www.raywenderlich.com/18782293-android-biometric-api-getting-started
        // TODO: Below code should likely not be here
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        builder.apply {
            if (allowDeviceCredential) setDeviceCredentialAllowed(true)
            else setNegativeButtonText("Cancel")
        }

        val prompt = builder.build()
    }
}

class PassActor(protected val sharedPreferences: SharedPreferences) : SecurityActor(TYPE_PASS) {

    constructor(context: Context) : this(context.getSharedPreferences(security_storage, Context.MODE_PRIVATE))
    constructor(activity: Activity) : this(activity.baseContext)

    private companion object {
        const val pass_storage_key = "SECURITY_PASS"
    }

    override fun actorAvailable(): VerificationMessage = VerificationMessage.createCorrect()

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
        if (!hasCredentials())
            return VerificationMessage.createNoInit()

        if (token !is PasswordVerificationToken)
            throw IllegalArgumentException("Password actor requires password verification token")

        val given: PasswordVerificationToken = token
        val known = sharedPreferences.getString(pass_storage_key, null)
            ?: throw IllegalStateException("Could not collect known password from system.")

        if (given.equals(known))
            return VerificationMessage.createCorrect()
        return VerificationMessage.createIncorrect()
    }
}