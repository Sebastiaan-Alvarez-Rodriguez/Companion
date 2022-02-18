package org.python.backend.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.IntDef
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.python.backend.util.CoroutineUtil
import timber.log.Timber

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(SecurityActor.TYPE_PASS, SecurityActor.TYPE_BIO)
annotation class SecurityType

interface SecurityInterface {
    /** security type being implemented by this actor */
    val type: @SecurityType Int

    /**
     * Whether this actor is available at this time.
     * @return Message with <code>message.type == SEC_CORRECT</code> and no body on success,
     * message with a human-readable error otherwise.
     */
    fun actorAvailable(): VerificationMessage

    /** Whether a credential has been set or not. */
    fun hasCredentials(): Boolean

    /** Setup a credential.
     * If a credential exists, this function calls <code>verify()</code>.
     * In such cases, caller must pass a verification object.
     * @param oldToken Optional object to call with verify-call. Required if a credential exists.
     * @param newToken Credentials to set.
     * @return Message providing status.
     * If 'correct', credentials were set/updated. Failure indication otherwise.
     */
    suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): VerificationMessage

    /**
     * Verifies given token for correctness.
     * @param token Token to verify. Can be <code>null</code>, as some actors do not need anything from users.
     * @return message indicating verification result status.
     */
    suspend fun verify(token: VerificationToken? = null): VerificationMessage
}

class SecurityActor : SecurityInterface {
    private var internalActor: SecurityInterface? = null
    var authenticated = MutableStateFlow(false)

    fun switchTo(activity: FragmentActivity, @SecurityType ofType: Int) {
        internalActor = when (ofType) {
            TYPE_PASS -> PassActor(activity)
            TYPE_BIO -> BioActor(activity)
            else -> null
        }
    }

    fun logout() {
        authenticated.value = false
    }

    private fun setAuthenticated(newValue: Boolean) = authenticated.update { newValue }

    override val type: Int = internalActor?.type ?: TYPE_UNDEFINED

    override fun actorAvailable(): VerificationMessage {
        return internalActor?.actorAvailable()
            ?: throw IllegalAccessException("Must set internal actor first")
    }

    override fun hasCredentials(): Boolean {
        return internalActor?.hasCredentials()
            ?: throw IllegalAccessException("Must set internal actor first")
    }

    override suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): VerificationMessage {
        return internalActor?.setCredentials(oldToken, newToken)
            ?: throw IllegalAccessException("Must set internal actor first")
    }

    override suspend fun verify(token: VerificationToken?): VerificationMessage {
        val msg = internalActor?.verify(token) ?: throw IllegalAccessException("Must set internal actor first")
        if (msg.type == VerificationMessage.SEC_CORRECT)
            setAuthenticated(true)
        return msg
    }

    companion object {
        const val TYPE_PASS = 0
        const val TYPE_BIO = 1
        /* Indicates that user has not yet set a preference */
        const val TYPE_UNDEFINED = -1
        const val security_storage = "SECURITY_ACTOR_STORAGE"
        const val preferred_actor_key = "SECURITY_ACTOR_PREFERRED"
        const val preferred_actor_default = TYPE_UNDEFINED
    }
}

internal class BioActor(
    private val activity: FragmentActivity,
    private var biometricPromptInfo: BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Log in")
            .setSubtitle("Log in to continue")
            .setDescription("")
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
) : SecurityInterface {

    fun updateBiometricPromptInfo(biometricPromptInfo: BiometricPrompt.PromptInfo) {
        this.biometricPromptInfo = biometricPromptInfo
    }

    override val type: Int = SecurityActor.TYPE_BIO

    override fun actorAvailable(): VerificationMessage {
        val biometricManager = BiometricManager.from(activity.baseContext)
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

    override fun hasCredentials(): Boolean = actorAvailable().type == VerificationMessage.SEC_CORRECT

    override suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): VerificationMessage = VerificationMessage.createCorrect()

    override suspend fun verify(token: VerificationToken?): VerificationMessage =
        CoroutineUtil.awaitCallback { verify(it) }

    private fun verify(callback: CoroutineUtil.Callback<VerificationMessage>) {
        initBiometricPrompt(
            activity = activity,
            onError = { errorCode, message ->
                callback.onResult(when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT, BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_TIMEOUT ->
                        VerificationMessage.createBadInput(body = StatusBody.BadInputBody(message))
                    BiometricPrompt.ERROR_LOCKOUT ->
                        VerificationMessage.createIncorrect(body = StatusBody.LockedBody(message))
                    BiometricPrompt.ERROR_HW_UNAVAILABLE ->
                        VerificationMessage.createUnavailable(body = StatusBody.UnavailableBody(message))
                    BiometricPrompt.ERROR_NO_BIOMETRICS ->
                        VerificationMessage.createNoInit(body = StatusBody.NoInitBody(message))
                    else ->
                        VerificationMessage.createOther(body = StatusBody.OtherBody(message))
                })
            },
            onSucces = { result -> callback.onResult(VerificationMessage.createCorrect())
            }
        ).authenticate(biometricPromptInfo)
    }

    private fun initBiometricPrompt(
        activity: FragmentActivity,
        onError: (Int, String) -> Unit,
        onSucces: (BiometricPrompt.AuthenticationResult) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.w("Biometric: Authentication failed for an unknown reason")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSucces(result)
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }
}

internal class PassActor(private val sharedPreferences: SharedPreferences) : SecurityInterface {

    constructor(context: Context) : this(context.getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE))
    constructor(activity: Activity) : this(activity.baseContext)

    private companion object {
        const val pass_storage_key = "SECURITY_PASS"
    }

    override val type: Int = SecurityActor.TYPE_PASS

    override fun actorAvailable(): VerificationMessage = VerificationMessage.createCorrect()

    override fun hasCredentials(): Boolean {
        return sharedPreferences.contains(pass_storage_key)
    }

    override suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): VerificationMessage {
        return CoroutineUtil.awaitSuspendingCallback { setCredentials(oldToken, newToken, it) }
    }

    private suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken, callback: CoroutineUtil.Callback<VerificationMessage>) {
        if (hasCredentials()) {
            val result = when (oldToken) {
                null -> throw IllegalArgumentException("Existing credentials detected. Caller must provide old verification token for verification.")
                else -> verify(oldToken)
            }
            if (result.type != VerificationMessage.SEC_CORRECT)
                return callback.onResult(result)
        }

        val preferencesEditor = sharedPreferences.edit()
        preferencesEditor.putString(pass_storage_key, newToken.toString())
        preferencesEditor.commit()

        return callback.onResult(VerificationMessage.createCorrect())
    }

    override suspend fun verify(token: VerificationToken?): VerificationMessage =
        CoroutineUtil.awaitCallback { verify(token, it) }

    private fun verify(token: VerificationToken?, callback: CoroutineUtil.Callback<VerificationMessage>) {
        if (!hasCredentials())
            return callback.onResult(VerificationMessage.createNoInit())

        if (token == null || token !is PasswordVerificationToken)
            throw IllegalArgumentException("Password actor requires password verification token")

        val given: PasswordVerificationToken = token
        val known = sharedPreferences.getString(pass_storage_key, null)
            ?: return callback.onResult(VerificationMessage.createNoInit())

        callback.onResult(when (given.equals(known)) {
            true -> VerificationMessage.createCorrect()
            false -> VerificationMessage.createIncorrect()
        })
    }
}