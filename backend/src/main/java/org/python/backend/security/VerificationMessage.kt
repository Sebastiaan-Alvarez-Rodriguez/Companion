package org.python.backend.security

import androidx.annotation.IntDef

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(
    VerificationMessage.SEC_CORRECT, VerificationMessage.SEC_INCORRECT,
    VerificationMessage.SEC_BADINPUT, VerificationMessage.SEC_LOCKED,
    VerificationMessage.SEC_NOINIT, VerificationMessage.SEC_OTHER
)
annotation class VerificationType

/**
 * Message containing verification status results.
 * @param type Verification type, indicating result status of the request (e.g. 'SEC_CORRECT').
 * @param body VerificationStatusBody, an optional object providing extra information.
 */
class VerificationMessage(@VerificationType val type: Int, val body: VerificationStatusBody? = null) {
    companion object {
        /** input correct */
        const val SEC_CORRECT = 0

        /** input incorrect */
        const val SEC_INCORRECT = 1

        /** input could not be validated (e.g. fingerprint misreading, biometric timeout) */
        const val SEC_BADINPUT = 2

        /** too many retries, authentication locked */
        const val SEC_LOCKED = 3

        /** authentication method not initialized */
        const val SEC_NOINIT = 4

        /** actor unavailable (e.g. fingerprint hardware already used by other app) */
        const val SEC_UNAVAILABLE = 5

        /** Other error */
        const val SEC_OTHER = -1


        fun createCorrect() = VerificationMessage(SEC_CORRECT)
        fun createIncorrect(body: VerificationStatusBody? = null) = VerificationMessage(SEC_INCORRECT, body)
        fun createBadInput(body: VerificationStatusBody? = null) = VerificationMessage(SEC_BADINPUT, body)
        fun createNoInit(body: VerificationStatusBody? = null) = VerificationMessage(SEC_NOINIT, body)
        fun createUnavailable(body: VerificationStatusBody? = null) = VerificationMessage(SEC_NOINIT, body)
        fun createOther(body: VerificationStatusBody? = null) = VerificationMessage(SEC_OTHER, body)
    }
}

/**
 * Verification message body, sent with a verification message.
 * Specific implementations may have additional members.
 * Always cast the body to the correct final type (using the <code>type</code> parameter from the message).
 * @param userMessage String Message to show to the user.
 */
abstract class VerificationStatusBody(val userMessage: String)

object StatusBody {
    class IncorrectBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Incorrect credentials.")
    class BadInputBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Could not read input.")
    class LockedBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Too many retries. Try again later.")
    class NoInitBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Authentication method not setup.")
    class UnavailableBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Authentication unavailable.")
    class OtherBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "An uncategorized problem occurred.")
}