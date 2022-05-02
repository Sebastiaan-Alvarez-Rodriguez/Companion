package org.python.security

import androidx.annotation.IntDef
import org.python.datacomm.Result
import org.python.datacomm.ResultType

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(
    VerificationResult.SEC_CORRECT, VerificationResult.SEC_INCORRECT,
    VerificationResult.SEC_BADINPUT, VerificationResult.SEC_LOCKED,
    VerificationResult.SEC_NOINIT, VerificationResult.SEC_OTHER
)
annotation class VerificationStatus

/**
 * Message containing verification status results.
 * @param type Result type, see [ResultType].
 * @param resultStatus Verification type, indicating result status of the request (e.g. 'SEC_CORRECT').
 * @param message Message explaining failure (in case of failure), or `null`.
 */
class VerificationResult(type: ResultType, @VerificationStatus val resultStatus: Int, message: String? = null) : Result(type, message) {
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

        fun from(@VerificationStatus securityType: Int) = VerificationResult(ResultType.SUCCESS, securityType)
    }
}