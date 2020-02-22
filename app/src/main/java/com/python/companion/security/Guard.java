package com.python.companion.security;

import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.security.Biometry.Biometry;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

// https://www.raywenderlich.com/778533-encryption-tutorial-for-android-getting-started
public class Guard {

    public void encryptKeystore(@NonNull String data, @NonNull String alias, Context context, @NonNull EncryptedCallback callback) {
        BiometricPrompt.CryptoObject obj = new BiometricPrompt.CryptoObject(getEncCipher(alias));
        Biometry bio = new Biometry.Builder().setSuccessCallback(authorizedCryptoObject -> {
            try {
                Cipher cipher = authorizedCryptoObject.getCipher();
                byte[] iv = cipher.getIV();
                byte[] encrypted = cipher.doFinal(Base64.getDecoder().decode(data));

                Log.i("TEST", "Encrypted: '"+ Base64.getEncoder().encodeToString(encrypted) +"'");

                callback.onFinish(Base64.getEncoder().encodeToString(encrypted), Base64.getEncoder().encodeToString(iv));
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                Log.i("BIO", "Weird exception: ", e);
                e.printStackTrace();
            }
        }).build(context, obj);
        bio.authorize();

    }

    private @Nullable Cipher getEncCipher(@NonNull String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            Log.i("ENCKEYSTORE", "Contains alias: "+keyStore.containsAlias(alias));
            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
            SecretKey key = secretKeyEntry.getSecretKey();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//            IvParameterSpec ivParameterSpec = generateCBCIV();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher;
        } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | KeyStoreException | NoSuchPaddingException | IOException e) {
            Log.i("ENCKEYSTORE", "Some exception occured: ", e);
//            e.printStackTrace();
        }
        return null;
    }

    public void decryptKeystore(@NonNull String data, @NonNull byte[] iv, @NonNull String alias, Context context, @NonNull DecryptedCallback callback) {
        BiometricPrompt.CryptoObject obj = new BiometricPrompt.CryptoObject(getDecCipher(iv, alias));
        Biometry bio = new Biometry.Builder().setSuccessCallback(authorizedCryptoObject -> {
            try {
                Cipher cipher = authorizedCryptoObject.getCipher();
                byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));

                Log.i("TEST", "Decrypted: '"+ Base64.getEncoder().encodeToString(decrypted)+"'");

                callback.onFinish(Base64.getEncoder().encodeToString(decrypted));
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                Log.i("BIO", "Weird exception: ", e);
                e.printStackTrace();
            }
        }).build(context, obj);
        bio.authorize();
    }

    public void decryptKeystore(@NonNull String data, @NonNull String iv, @NonNull String alias, Context context, @NonNull DecryptedCallback callback) {
        decryptKeystore(data, Base64.getDecoder().decode(iv), alias, context, callback);
    }

    public @Nullable Cipher getDecCipher(@NonNull byte[] iv, @NonNull String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
            SecretKey key = secretKeyEntry.getSecretKey();
            //TODO get IV here

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return cipher;
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IOException e) {
            Log.i("DECKEYSTORE", "Some exception occured: ", e);
        }
        return null;
    }

    private void generatePasswordBasedAESAndroidKeystore(@NonNull String alias) {
        try {
            KeyGenerator k = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keyGenParameterSpec =
                    new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
//                    .setUserAuthenticationValidityDurationSeconds(120)
                            .setUserAuthenticationValidityDurationSeconds(-1) // Always use fingerprint. Needed for setInvalidatedByBiometricEnrollment()
                            .setRandomizedEncryptionRequired(true)
                            .setUserAuthenticationRequired(true) //WARNING: Invalidates keys if fingerprint enrolled/removed, pass changed etc.
                            .setInvalidatedByBiometricEnrollment(false)
                            .build();
            k.init(keyGenParameterSpec);
            SecretKey key = k.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            Log.i("GENKEYSTORE", "Problem in Keygenerator.getInstance()", e);
        } catch (InvalidAlgorithmParameterException e) {
            Log.i("GENKEYSTORE", "Problem in Keygenerator.init()", e);
        }
    }


    public void generatePasswordBasedAES(@NonNull String data, @NonNull String userPassword) {
        try {
            SecretKeySpec secretKeySpec = generateSecretAESKey(userPassword.toCharArray());
            IvParameterSpec ivParameterSpec = generateCBCIV();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            cipher.doFinal(data.getBytes());
        } catch (NoSuchAlgorithmException e) {
            Log.i("PBE", "Exception problem SecretKeyFactory.getInstance(): ", e);
        } catch (InvalidKeySpecException e) {
            Log.i("PBE", "Exception problem at secretKeyFactory.generateSecret(): ", e);

        } catch (NoSuchPaddingException e) {
            Log.i("PBE", "Exception problem at Cipher.getInstance(): ", e);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Log.i("PBE", "Exception problem at cipher.init(): ", e);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Log.i("PBE", "Exception problem at cipher.dofinal(): ", e);
        }
    }

    private @NonNull byte[] generateSecureSalt(int length) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return salt;
    }

    private SecretKeySpec generateSecretAESKey(@NonNull char[] userPassword) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(userPassword, generateSecureSalt(256), 2048, 256);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private IvParameterSpec generateCBCIV() {
        byte[] iv = generateSecureSalt(16);
        return new IvParameterSpec(iv);
    }
}
