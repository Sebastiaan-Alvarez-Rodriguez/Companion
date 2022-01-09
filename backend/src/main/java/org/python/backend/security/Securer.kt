package org.python.backend.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

data class SecureResult(val data: ByteArray, val iv: ByteArray) {
    fun dataString(): String = Base64.encodeToString(data, Base64.DEFAULT)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SecureResult

        if (!data.contentEquals(other.data)) return false
        if (!iv.contentEquals(other.iv)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}
object Securer {
    /**
     * Encrypt given string using key in Android KeyStore with given alias.
     * @param data To-be enrypted data
     * @param
     * @return Pair of encrypted string, iv
     */
    fun encrypt(data: ByteArray, alias: String): SecureResult {
        val cipher: Cipher = getEncCipher(alias)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return SecureResult(data = encrypted, iv = iv)
    }

    fun encrypt(data: String, alias: String): SecureResult =
        encrypt(
            data = data.toByteArray(StandardCharsets.UTF_8),
            alias = alias
        )

    /**
     * Gets cipher object from Android Keystore, which may be used to encrypt a note
     * @param alias name where key was stored under in Android Keystore
     * @return cipher object, ready for encryption
     */
    private fun getEncCipher(alias: String): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(alias))
            if (!generateAES(alias))
                throw KeyStoreException("Could not create key for alias '$alias'")
        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val key = secretKeyEntry.secretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Generate AES key, and store it under given alias. All keys are stored in Android KeyStore.
     * Overrides keys if alias already exists.
     * @param alias Name to store key under
     * @return `true` on success, `false` otherwise
     */
    @Synchronized
    private fun generateAES(alias: String): Boolean {
        try {
            val k = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true) // Enforces randomized encryption
                .setUserAuthenticationRequired(false) // Requires user authentication. Invalidates when fingerprint enrolled/removed, pass changed
                .setInvalidatedByBiometricEnrollment(false) // States that we do not invalidate key if fingerprint is enrolled
                .setUnlockedDeviceRequired(true) // Requires screen to be unlocked to be able to use key
                .build()
            k.init(keyGenParameterSpec)
            k.generateKey()
            return true
        } catch (e: NoSuchAlgorithmException) {
            // Invalid algorithm (only possible if OS does not support AES or GCM) or no AndroidKeyStore
            Timber.e("generateAES", "Problem in Keygenerator.getInstance()", e)
        } catch (e: NoSuchProviderException) {
            Timber.e("generateAES", "Problem in Keygenerator.getInstance()", e)
        } catch (e: InvalidAlgorithmParameterException) {
            Timber.e("generateAES", "Problem in Keygenerator.init()", e)
        }
        return false
    }
}