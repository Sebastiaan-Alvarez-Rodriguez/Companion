package org.python.security

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.IntDef
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.security.util.CoroutineUtil
import timber.log.Timber

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(SecurityActor.TYPE_PASS, SecurityActor.TYPE_BIO)
annotation class SecurityType

val SecurityTypes: @SecurityType IntArray = intArrayOf(
    SecurityActor.TYPE_PASS,
    SecurityActor.TYPE_BIO
)

@Suppress("unused", "MemberVisibilityCanBePrivate")
data class CompactSecurityTypeArray(val types: Int) {
    constructor(types: Int?) : this(types ?: default)

    fun isAllowed(type: @SecurityType Int): Boolean = !isForbidden(type)
    fun isForbidden(type: @SecurityType Int): Boolean = types and type == 0

    fun allowed(): Set<@SecurityType Int> = SecurityTypes.filter { type -> type and types > 0 }.toSet()
    fun forbidden(): Set<@SecurityType Int> = SecurityTypes.filter { type -> type and types == 0 }.toSet()

    override fun toString(): String = types.toString()

    companion object {
        const val default = Int.MAX_VALUE
        fun create(vararg allowed: @SecurityType Int): CompactSecurityTypeArray = CompactSecurityTypeArray(allowed.reduce(Int::or))
        fun create(allowed: Collection<@SecurityType Int>): CompactSecurityTypeArray = CompactSecurityTypeArray(allowed.reduce(Int::or))
    }
}

/** Shared components between public-facing and internal interface */
interface SecurityInterfaceBase {
    /** security type being implemented by this actor */
    val type: @SecurityType Int

    /**
     * Whether this actor is available at this time.
     * @return `true` when actor is available, `false` otherwise.
     */
    fun actorAvailable(): Result

    /** Whether a credential has been set or not. */
    fun hasCredentials(): Boolean

    /**
     * Verifies given token for correctness.
     * @param token Token to verify. Can be <code>null</code>, as some actors do not need anything from users.
     * @return Verification result indicating new status.
     */
    suspend fun verify(token: VerificationToken? = null): VerificationResult
}

/** Public-facing interface */
interface SecurityMetaInterface : SecurityInterfaceBase {
    /** Setup a credential.
     * If a credential exists, this function calls <code>verify()</code>.
     * In such cases, caller must pass a verification object.
     * @param oldToken Optional object to call with verify-call. Required if a credential exists.
     * @param newToken Credentials to set.
     * @return Result object providing status.
     * If 'correct', credentials were set/updated. Failure indication otherwise.
     */
    suspend fun setCredentials(oldToken: VerificationToken? = null, newToken: VerificationToken): Result
}

/** Internal interface */
internal interface SecurityInterfaceInternal : SecurityInterfaceBase {
    /** Setup a credential.
     * If a credential exists, this function calls <code>verify()</code>.
     * In such cases, caller must pass a verification object.
     * @param oldToken Optional object to call with verify-call. Required if a credential exists.
     * @param newToken Credentials to set.
     * @param clearance Current clearance level.
     * @return Message providing status.
     * If 'correct', credentials were set/updated. Failure indication otherwise.
     */
    suspend fun setCredentials(oldToken: VerificationToken? = null, newToken: VerificationToken, clearance: Int): Result
}

class SecurityActor : SecurityMetaInterface {
    private var internalActor: SecurityInterfaceInternal? = null
    override val type: Int = internalActor?.type ?: TYPE_UNDEFINED

    val clearance = MutableStateFlow<Int>(0)

    private val mutex = Mutex()
    private lateinit var activity: FragmentActivity

    fun load(activity: FragmentActivity) {
        if (!this::activity.isInitialized)
           this.activity = activity
    }

    suspend fun <T> withLock(action: () -> T): T {
        mutex.lock()
        val result = action()
        mutex.unlock()
        return result
    }

    fun switchTo(@SecurityType type: Int) {
        internalActor = getActorOfType(activity, type)
    }

    fun logout() = setClearanceLevel(0)

    private fun setClearanceLevel(newValue: Int) = clearance.update { newValue }

    override fun actorAvailable(): Result = internalActor?.actorAvailable() ?: Result(ResultType.FAILED, "Internal problem with security actor")

    override fun hasCredentials(): Boolean = internalActor?.hasCredentials() ?: throw IllegalAccessException("Internal problem with security actor")

    override suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken): Result {
        val hasAnyCredentials = clearance.value == 0 && SecurityTypes.any { type -> getActorOfType(activity, type)?.hasCredentials() ?: false }
        if (hasAnyCredentials)
            return Result(ResultType.FAILED, "Cannot reset credentials: Another method has already been setup. Login first using that method.")
        return internalActor?.setCredentials(oldToken, newToken, clearance.value) ?: Result(ResultType.FAILED, "Internal problem with security actor")
    }

    ///// Information section
    inline fun canLogin() = hasCredentials()
    fun canSetup() = !hasCredentials() && (clearance.value > 0 || !hasAnyCredentials())
    fun canReset() = hasCredentials() && clearance.value > 0

    fun notSetupMethods() = SecurityTypes.filter { !(getActorOfType(activity, it)?.hasCredentials() ?: false) }
    fun setupMethods() = SecurityTypes.filter { getActorOfType(activity, it)?.hasCredentials() ?: false }

    /** Returns name of security method for given type(s). */
    fun methodName() = methodName(type)
    fun methodName(@SecurityType types: List<Int>) = types.map { methodName(it) }
    fun methodName(@SecurityType type: Int) = when (type) {
        TYPE_BIO -> "Fingerprint"
        TYPE_PASS -> "Password"
        else -> throw IllegalArgumentException("Could not find security type for $type")
    }

    /** Returns `true` if any security actor exists with setup credentials, `false` otherwise */
    private fun hasAnyCredentials(): Boolean = SecurityTypes.any { type -> getActorOfType(activity, type)?.hasCredentials() ?: false }

    //// End Information section

    override suspend fun verify(token: VerificationToken?): VerificationResult {
        val msg = internalActor?.verify(token) ?: throw IllegalAccessException("Internal problem with security actor")
        if (msg.type == ResultType.SUCCESS && msg.resultStatus == VerificationResult.SEC_CORRECT)
            setClearanceLevel(1)
        return msg
    }

    companion object {
        const val TYPE_PASS = 1
        const val TYPE_BIO = 2

        /* Indicates that user has not yet set a preference */
        const val TYPE_UNDEFINED = -1
        const val security_storage = "SECURITY_ACTOR_STORAGE" // dictionary key for security-related storage

        private fun getActorOfType(activity: FragmentActivity, type: @SecurityType Int) = when (type) {
            TYPE_PASS -> PassActor(activity)
            TYPE_BIO -> BioActor(activity)
            else -> null
        }
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
) : SecurityInterfaceInternal {

    fun updateBiometricPromptInfo(biometricPromptInfo: BiometricPrompt.PromptInfo) {
        this.biometricPromptInfo = biometricPromptInfo
    }

    override val type: Int = SecurityActor.TYPE_BIO

    override fun actorAvailable(): Result {
        val biometricManager = BiometricManager.from(activity.baseContext)
        return when (val status = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Result.DEFAULT_SUCCESS
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Result(ResultType.FAILED, "Biometrics hardware in use by other app.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Result(ResultType.FAILED,"No biometrics enrolled")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Result(ResultType.FAILED,"No biometrics hardware detected")
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Result(ResultType.FAILED, "Biometrics hardware does not satisfy this app's requirements")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Result(ResultType.FAILED, "Outdated software for biometrics hardware detected. Update required.")
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Result(ResultType.FAILED, "Unknown error occurred. Please try again.")
            else -> {
                Timber.w("actorAvailable: BioActor: Received unknown biometrics status code '$status'")
                Result(ResultType.FAILED, "Unknown error occurred. Please try again.")
            }
        }
    }

    override fun hasCredentials(): Boolean =
        BiometricManager.from(activity.baseContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

    override suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken, clearance: Int): Result = Result.DEFAULT_SUCCESS

    override suspend fun verify(token: VerificationToken?): VerificationResult = verify()

    suspend fun verify(): VerificationResult {
        return withContext(Dispatchers.Main) {
            CoroutineUtil.awaitCallback { callback: CoroutineUtil.Callback<VerificationResult> ->
                initBiometricPrompt(
                    activity = activity,
                    onError = { errorCode, message ->
                        callback.onResult(when (errorCode) {
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT, BiometricPrompt.ERROR_CANCELED,
                            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_TIMEOUT -> VerificationResult(ResultType.FAILED, VerificationResult.SEC_BADINPUT, message)
                            BiometricPrompt.ERROR_LOCKOUT -> VerificationResult(ResultType.FAILED, VerificationResult.SEC_LOCKED, message)
                            BiometricPrompt.ERROR_HW_UNAVAILABLE -> VerificationResult(ResultType.FAILED, VerificationResult.SEC_UNAVAILABLE, message)
                            BiometricPrompt.ERROR_NO_BIOMETRICS -> VerificationResult(ResultType.FAILED, VerificationResult.SEC_NOINIT, message)
                            else -> VerificationResult(ResultType.FAILED, VerificationResult.SEC_OTHER, message)
                        })
                    },
                    onSuccess = { callback.onResult(VerificationResult.from(VerificationResult.SEC_CORRECT)) }
                ).authenticate(biometricPromptInfo)
            }
        }
    }

    private fun initBiometricPrompt(
        activity: FragmentActivity,
        onError: (Int, String) -> Unit,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
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
                onSuccess(result)
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }
}

internal class PassActor(private val sharedPreferences: SharedPreferences) : SecurityInterfaceInternal {

    constructor(context: Context) : this(context.getSharedPreferences(SecurityActor.security_storage, Context.MODE_PRIVATE))
    constructor(activity: Activity) : this(activity.baseContext)

    companion object {
        const val hash_security_key = "PASS_ACTOR_HASH"
        const val salt_storage_key = "PASS_ACTOR_SALT"
    }

    override val type: Int = SecurityActor.TYPE_PASS

    override fun actorAvailable(): Result = Result.DEFAULT_SUCCESS

    override fun hasCredentials(): Boolean = sharedPreferences.contains(hash_security_key)

    @SuppressLint("ApplySharedPref")
    override suspend fun setCredentials(oldToken: VerificationToken?, newToken: VerificationToken, clearance: Int): Result = withContext(Dispatchers.Default) {
        when (hasCredentials() && clearance == 0) { // must provide old pass when old pass exists && not logged in
            true -> when (oldToken) {
                null -> VerificationResult(
                    ResultType.FAILED,
                    VerificationResult.SEC_INCORRECT,
                    "Existing credentials detected. Caller must provide old verification token for verification."
                )
                else -> verify(oldToken)
            }
            else -> Result.DEFAULT_SUCCESS
        }.pipe {
            if (newToken !is PasswordVerificationToken)
                return@pipe VerificationResult(ResultType.FAILED, VerificationResult.SEC_BADINPUT)

            val preferencesEditor = sharedPreferences.edit()
            preferencesEditor.putString(hash_security_key, newToken.hashString())
            preferencesEditor.putString(salt_storage_key, newToken.saltString())
            preferencesEditor.commit()
            Result.DEFAULT_SUCCESS
        }
    }

    override suspend fun verify(token: VerificationToken?): VerificationResult = withContext(Dispatchers.Default) { verifyInternal(token) }

    private suspend fun verifyInternal(token: VerificationToken?): VerificationResult = withContext(Dispatchers.Default) {
        if (token == null || token !is PasswordVerificationToken)
            return@withContext VerificationResult(ResultType.FAILED, VerificationResult.SEC_OTHER,"Password actor requires password verification token")

        val given: PasswordVerificationToken = token
        val known = PasswordVerificationToken.fromString(
            hash = sharedPreferences.getString(hash_security_key, null)
                ?: return@withContext VerificationResult(ResultType.FAILED, VerificationResult.SEC_NOINIT, "Did not initialize security type"),
            salt = sharedPreferences.getString(salt_storage_key, null)
                ?: return@withContext VerificationResult(ResultType.FAILED, VerificationResult.SEC_NOINIT, "Did not initialize salt")
        )

        return@withContext when (given == known) {
            true -> VerificationResult(ResultType.SUCCESS, VerificationResult.SEC_CORRECT)
            else -> VerificationResult(ResultType.FAILED, VerificationResult.SEC_INCORRECT, "Incorrect Password")
        }
    }
}