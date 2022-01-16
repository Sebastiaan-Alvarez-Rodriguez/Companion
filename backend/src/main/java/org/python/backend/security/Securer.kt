package org.python.backend.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

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
     * Encrypt given bytes using key in Android KeyStore with given alias.
     * @param data To-be encrypted data
     * @param alias Alias name to store key under.
     * @return Pair of encrypted string, iv on success, `null` otherwise.
     */
    fun encrypt(data: ByteArray, alias: String): SecureResult? {
        val cipher: Cipher = getEncCipher(alias) ?: return null
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return SecureResult(data = encrypted, iv = iv)
    }

    /**
     * Encrypt given string using key in Android KeyStore with given alias.
     * @see encrypt(ByteArray, String)
     */
    fun encrypt(data: String, alias: String): SecureResult? =
        encrypt(
            data = data.toByteArray(StandardCharsets.UTF_8),
            alias = alias
        )

    /**
     * Decrypt given String using key in Android KeyStore with given alias.
     * @return decrypted string on success, `null` otherwise.
     */
    fun decrypt(data: ByteArray, iv: ByteArray, alias: String): String? {
        val cipher: Cipher = getDecCipher(iv, alias) ?: return null
        val decrypted = cipher.doFinal(data)
        return String(decrypted, StandardCharsets.UTF_8)
    }
    fun decrypt(data: String, iv: ByteArray, alias: String): String? =
        decrypt(Base64.decode(data, Base64.DEFAULT), iv, alias)

    fun changeAlias(oldKey: String, updatedData: String, updatedKey: String): Boolean {
        if (oldKey == updatedKey)
            return true
        deleteAlias(oldKey)
        return encrypt(updatedData, updatedKey) != null
    }
    /**
     * Deletes the given alias key from the keystore.
     * @note Warning: This is a destructive, non-recoverable operation.
     * @param alias Alias to remove
     */
    fun deleteAlias(alias: String) {
        getKeyStore()?.deleteEntry(alias)
    }

    /**
     * Gets cipher object from Android Keystore, to encrypt data.
     * @param alias Name to store key under in Android Keystore.
     * @return cipher object, ready for encryption.
     */
    private fun getEncCipher(alias: String): Cipher? {
        val keyStore = getKeyStore() ?: return null
        return try {
            if (!keyStore.containsAlias(alias))
                if (!generateAES(alias))
                    throw KeyStoreException("Could not create key for alias '$alias'")
            val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(Cipher.ENCRYPT_MODE, secretKeyEntry.secretKey)
            }
        } catch (e: Exception) {
            Timber.e(e, "Problem in getEncCipher")
            null
        }
    }

    /**
     * Gets cipher object from Android Keystore, to decrypt data.
     * @param iv Initialization Vector used when encrypting data.
     * @param alias name under which key was stored in Android Keystore.
     * @return cipher object, ready for decryption on success, `null` otherwise.
     */
    private fun getDecCipher(iv: ByteArray, alias: String): Cipher? {
        val keyStore = getKeyStore() ?: return null
        return try {
            val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(Cipher.DECRYPT_MODE, secretKeyEntry.secretKey, GCMParameterSpec(128, iv))
            }
        } catch (e: Exception) {
            Timber.e(e, "Problem in getDecCipher")
            null
        }
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
            Timber.e(e, "Problem in Keygenerator.getInstance()")
        } catch (e: NoSuchProviderException) {
            Timber.e(e, "Problem in Keygenerator.getInstance()")
        } catch (e: InvalidAlgorithmParameterException) {
            Timber.e(e, "Problem in Keygenerator.init()")
        }
        return false
    }

    /** Prepares keyStore. When encountering exceptions, returns `null`. */
    private fun getKeyStore(): KeyStore? {
        return try {
            KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
        } catch (e: Exception) {
            Timber.e(e, "Problem in getKeyStore")
            null
        }
    }
}