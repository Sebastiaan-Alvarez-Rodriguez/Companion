package org.python.backend.security

import androidx.annotation.IntDef


const val SEC_CORRECT = 0
const val SEC_INCORRECT = 1
const val SEC_BADINPUT = 2
const val SEC_LOCKED = 3

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(SEC_CORRECT, SEC_INCORRECT, SEC_BADINPUT, SEC_LOCKED)
annotation class VerificationType

/**
 * Message containing verification status results.
 * @param type Verification type, indicating result status of the request (e.g. 'SEC_CORRECT').
 * @param body VerificationStatusBody, an optional object providing extra information.
 */
class VerificationMessage(@VerificationType val type: Int, val body: VerificationStatusBody? = null) {
    companion object {
        fun createCorrect() = VerificationMessage(SEC_CORRECT)
    }
}

/**
 * Verification message body, sent with a verification message.
 * Specific implementations may have additional members.
 * Always cast the body to the correct final type (using the <code>type</code> parameter from the message).
 * @param userMessage String Message to show to the user.
 */
abstract class VerificationStatusBody(userMessage: String)

object StatusBody {
    class IncorrectBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Incorrect credentials.")
    class BadInputBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Could not read input.")
    class LockedBody(userMessage: String?) : VerificationStatusBody(userMessage ?: "Too many retries. Try again later.")
}