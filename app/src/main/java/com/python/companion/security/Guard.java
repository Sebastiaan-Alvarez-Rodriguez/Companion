package com.python.companion.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// https://www.raywenderlich.com/778533-encryption-tutorial-for-android-getting-started

// put/get password from android keystore
// https://www.programcreek.com/java-api-examples/?code=opensecuritycontroller/osc-core/osc-core-master/osc-server/src/main/java/org/osc/core/broker/util/crypto/KeyStoreProvider.java

public abstract class Guard {
    protected boolean validated = false;

    protected abstract void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull ValidateCallback validateCallback);

    /**
     * Public call to handle encryption once
     * @see Guard#encryptInternal(String, String, EncryptedCallback)
     * @param data Data to encrypt
     * @param alias Name we stored a key under
     * @param callback Called when encryption had success
     */
    public void encrypt(@NonNull String data, @NonNull String alias, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull EncryptedCallback callback) {
        if (!this.validated) {
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    encryptInternal(data, alias, callback);
                }

                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });
        } else {
            encryptInternal(data, alias, callback);
        }
    }

    /**
     * Public call to handle decryption once
     * @see Guard#decryptInternal(String, byte[], String, DecryptedCallback)
     * @param data Data to decrypt
     * @param iv Initialization Vector (IV) we got in the callback while encrypting ({@link Guard#encrypt(String, String, FragmentManager, Context, EncryptedCallback)}
     * @param alias Name we stored key under
     * @param callback Called when decryption had success
     */
    public void decrypt(@NonNull String data, @NonNull byte[] iv, @NonNull String alias, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull DecryptedCallback callback) {
        if (!this.validated) {
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    decryptInternal(data, iv, alias, callback);
                }

                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });
        } else {
            decryptInternal(data, iv, alias, callback);
        }
    }

    /**
     * Encrypt given string using key in Android KeyStore with given alias, and return content in a {@link EncryptedCallback}
     */
    @SuppressWarnings("ConstantConditions")
    private void encryptInternal(@NonNull String data, @NonNull String alias, @NonNull EncryptedCallback callback) {
        try {
            Pair<String, byte[]> encrypted = enc(data, alias);
            callback.onFinish(encrypted.first, encrypted.second);
        } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | KeyStoreException | NoSuchPaddingException | IOException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e("encrypt", "Exception: ", e);
            callback.onFailure();
        }
    }

    /**
     * Encrypts multiple data objects at once
     * @see Guard#encryptInternal(String, String, EncryptedCallback)
     */
    @SuppressWarnings("ConstantConditions")
    protected void encryptInternal(@NonNull List<String> datas, @NonNull List<String> aliases, @NonNull EncryptedCallback callback) {
        if (datas.size() != aliases.size())
            throw new RuntimeException("Data length is "+datas.size()+", aliases length is "+aliases.size()+", but should have same length!");
        for (int x = 0; x < datas.size(); ++x) {
            try {
            Pair<String, byte[]> encrypted = enc(datas.get(x), aliases.get(x));
            callback.onFinish(encrypted.first, encrypted.second);
            } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | KeyStoreException | NoSuchPaddingException | IOException | BadPaddingException | IllegalBlockSizeException e) {
                Log.e("encrypt", "Exception: ", e);
                callback.onFailure();
            }
        }
    }

    /**
     * Decrypt given String using key in Android KeyStore with given alias, and return content in a {@link DecryptedCallback}
     */
    protected void decryptInternal(@NonNull String data, @NonNull byte[] iv, @NonNull String alias, @NonNull DecryptedCallback callback) {
        try {
            callback.onFinish(dec(data, iv, alias));
        } catch (InvalidKeyException | UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | IOException e) {
            Log.e("decrypt", "Exception: ", e);
            callback.onFailure();
        }
    }

    /**
     * Decrypts multiple data objects at once
     * @see Guard#decryptInternal(String, byte[], String, DecryptedCallback)
     */
    protected void decryptInternal(@NonNull List<String> datas, @NonNull List<byte[]> ivs, @NonNull List<String> aliases, @NonNull DecryptedCallback callback) {
        if (datas.size() != ivs.size() || datas.size() != aliases.size())
            throw new RuntimeException("Data length is "+datas.size()+", ivs length is "+ivs.size()+", aliases length is "+aliases.size()+", but all should have same length!");
        for (int x = 0; x < datas.size(); ++x) {
            try {
                callback.onFinish(dec(datas.get(x), ivs.get(x), aliases.get(x)));
            } catch (InvalidKeyException | UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | IOException e) {
                Log.e("decrypt", "Exception: ", e);
                callback.onFailure();
            }
        }
    }
    
    /**
     * Encrypt given string using key in Android KeyStore with given alias, and return content in a {@link EncryptedCallback}
     * @return Pair of encrypted string, iv
     */
    private Pair<String, byte[]> enc(@NonNull String data, @NonNull String alias) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
        if (!this.validated)
            throw new RuntimeException("Cannot perform operation: Not authenticated");
        Cipher cipher = getEncCipher(alias);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return new Pair<>(Base64.encodeToString(encrypted, Base64.DEFAULT), iv);
    }
    
    /**
     * Decrypt given String using key in Android KeyStore with given alias, and return content in a {@link DecryptedCallback}
     * @return decrypted string
     */
    private String dec(@NonNull String data, @NonNull byte[] iv, @NonNull String alias) throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnrecoverableEntryException, IOException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = getDecCipher(iv, alias);
        byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    /**
     * Gets cipher object from Android Keystore, which may be used to encrypt a note
     * @param alias name where key was stored under in Android Keystore
     * @return cipher object, ready for encryption
     */
    private static @NonNull Cipher getEncCipher(@NonNull String alias)  throws InvalidKeyException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, KeyStoreException, NoSuchPaddingException, IOException {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias(alias))
                if (!generateAES(alias))
                    throw new KeyStoreException("Could not create key for alias '"+alias+"'");
                
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
            SecretKey key = secretKeyEntry.getSecretKey();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
    }

    /**
     * Gets cipher object from Android Keystore, which may be used to decrypt a note
     * @param iv Initialization Vector (IV) used when encrypting note
     * @param alias name where key was stored under in Android Keystore
     * @return cipher object, ready for decryption
     */
    private static @NonNull Cipher getDecCipher(@NonNull byte[] iv, @NonNull String alias) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
        SecretKey key = secretKeyEntry.getSecretKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher;
    }
    
    /**
     * Generate AES key, and store it under given alias. All keys are stored in Android KeyStore. Overrides keys if alias already exists
     * @param alias Name to store key under
     * @return {@code true} on success, {@code false} otherwise
     */
    private static boolean generateAES(@NonNull String alias) {
        try {
            KeyGenerator k = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keyGenParameterSpec =
                    new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(true) // Enforces randomized encryption
                            .setUserAuthenticationRequired(false) // Requires user authentication. Invalidates when fingerprint enrolled/removed, pass changed
                            .setInvalidatedByBiometricEnrollment(false) // States that we do not invalidate key if fingerprint is enrolled
                            .setUnlockedDeviceRequired(true) // Requires screen to be unlocked to be able to use key
                            .setIsStrongBoxBacked(true) // Should be backed by encryption hardware
                            .build();
            k.init(keyGenParameterSpec);
            k.generateKey();
            return true;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // Invalid algorithm (only possible if OS does not support AES or GCM) or no AndroidKeyStore
            Log.e("generateAES", "Problem in Keygenerator.getInstance()", e);
        } catch (InvalidAlgorithmParameterException e) {
            Log.e("generateAES", "Problem in Keygenerator.init()", e);
        }
        return false;
    }
}
