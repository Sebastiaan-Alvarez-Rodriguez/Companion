package com.python.companion.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;

import com.python.companion.R;
import com.python.companion.security.biometry.BioGuard;
import com.python.companion.security.password.PassGuard;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.stream.Stream;

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
    public static final int TYPE_PASSGUARD = 0;
    public static final int TYPE_BIOGUARD = 1;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_PASSGUARD, TYPE_BIOGUARD})
    public @interface Type {}

    private static volatile Guard INSTANCE;
    private static volatile @Type int guardtype = TYPE_PASSGUARD;


    /** Initializes Guard by setting the correct guardtype. Guard objects constructed at a later point will be constructed with correct type */
    public static void init(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        Guard.guardtype = preferences.getInt("GuardType", Guard.TYPE_PASSGUARD);
    }

    /**
     * Gets guard of specified type. If we currently use another type, we switch instance type and reset validation
     * @param type Type of guard to return
     */
    public static Guard getGuard(@Type int type) {
        if (INSTANCE == null) {
            synchronized (Guard.class) {
                if (INSTANCE == null) {
                    INSTANCE = type == TYPE_PASSGUARD ? new PassGuard() : new BioGuard();
                    Guard.guardtype = type;
                }
            }
        } else if (Guard.guardtype != type) {
            synchronized (Guard.class) {
                if (Guard.guardtype != type) {
                    INSTANCE = type == TYPE_PASSGUARD ? new PassGuard() : new BioGuard();
                    Guard.guardtype = type;
                }
            }
        }
        return INSTANCE;
    }

    /** Gets guard of current type */
    public static Guard getGuard() {
        if (INSTANCE == null) {
            synchronized (Guard.class) {
                if (INSTANCE == null)
                    INSTANCE = Guard.guardtype == TYPE_PASSGUARD ? new PassGuard() : new BioGuard();
            }
        }
        return INSTANCE;
    }

    public synchronized static void setGuardType(@Type int type) {
        if (Guard.guardtype != type) {
            INSTANCE = type == TYPE_PASSGUARD ? new PassGuard() : new BioGuard();
            Guard.guardtype = type;
        }
    }

    protected Guard() {}
    protected boolean validated = false;

    protected abstract void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull ValidateCallback validateCallback);

    /**
     * Public call to handle encryption once
     * @see Guard#encryptInternal(String, String, EncryptedCallback)
     * @param data Data to encrypt
     * @param alias Name we stored a key under
     * @param callback Called when encryption had success
     */
    public synchronized void encrypt(@NonNull String data, @NonNull String alias, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull EncryptedCallback callback) {
        if (!this.validated) {
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    synchronized (Guard.class) {
                        final boolean wasvalidated = Guard.this.validated;
                        Guard.this.validated = true;
                        encryptInternal(data, alias, callback);
                        if (!wasvalidated)
                            Guard.this.validated = false;
                    }
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
     * Public call to handle encryption of multiple items at once. For each item, the callback will be called
     * @see Guard#encrypt(String, String, FragmentManager, Context, EncryptedCallback)
     */
    public synchronized void encrypt(@NonNull Stream<String> datas, @NonNull Stream<String> aliases, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull EncryptedCallback callback) {
        if (!this.validated) {
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    synchronized (Guard.class) {
                        final boolean wasvalidated = Guard.this.validated;
                        Guard.this.validated = true;
                        encryptInternal(datas, aliases, callback);
                        if (!wasvalidated)
                            Guard.this.validated = false;
                    }
                }
                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });
        } else {
            encryptInternal(datas, aliases, callback);
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
    public synchronized void decrypt(@NonNull String data, @NonNull byte[] iv, @NonNull String alias, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull DecryptedCallback callback) {
        if (!this.validated) {
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    synchronized (Guard.class) {
                        final boolean wasvalidated = Guard.this.validated;
                        Guard.this.validated = true;
                        decryptInternal(data, iv, alias, callback);
                        if (!wasvalidated)
                            Guard.this.validated = false;
                    }
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
     * Public call to handle decryption of multiple items at once. For each item, the callback will be called
     * @see Guard#decrypt(String, byte[], String, FragmentManager, Context, DecryptedCallback)
     */
    public synchronized void decrypt(@NonNull Stream<String> datas, @NonNull Stream<byte[]> ivs, @NonNull Stream<String> aliases, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull DecryptedCallback callback) {
        if (!this.validated) {
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    synchronized (Guard.class) {
                        final boolean wasvalidated = Guard.this.validated;
                        Guard.this.validated = true;
                        // This would be a major security vulnerability, since a thread could be used to boot other functions while this thread is busy.
                        // Luckily, every method is synchronized, meaning we have a lock, and only 1 method can be active at any point in time.
                        // When control reaches this statement, we own the lock. No abuse is possible.
                        decryptInternal(datas, ivs, aliases, callback);
                        if (!wasvalidated)
                            Guard.this.validated = false;
                    }
                }

                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });
        } else {
            decryptInternal(datas, ivs, aliases, callback);
        }
    }

    /**
     * Encrypt given string using key in Android KeyStore with given alias, and return content in a {@link EncryptedCallback}
     */
    @SuppressWarnings("ConstantConditions")
    private synchronized void encryptInternal(@NonNull String data, @NonNull String alias, @NonNull EncryptedCallback callback) {
        try {
            Pair<String, byte[]> encrypted = enc(data, alias);
            callback.onFinish(encrypted.first, encrypted.second);
        } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | KeyStoreException | NoSuchPaddingException | IOException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e("encrypt", "Exception: ", e);
            callback.onFailure();
        }
    }

    /**
     * Encrypts multiple data objects at once. Make absolutely sure the streams are of even length.
     * @see Guard#encryptInternal(String, String, EncryptedCallback)
     */
    @SuppressWarnings("ConstantConditions")
    protected synchronized void encryptInternal(@NonNull Stream<String> datas, @NonNull Stream<String> aliases, @NonNull EncryptedCallback callback) {
        Iterator<String> dit = datas.iterator();
        Iterator<String> aliasit = aliases.iterator();
        while (dit.hasNext()) {
            try {
                Pair<String, byte[]> encrypted = enc(dit.next(), aliasit.next());
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
    protected synchronized void decryptInternal(@NonNull String data, @NonNull byte[] iv, @NonNull String alias, @NonNull DecryptedCallback callback) {
        try {
            callback.onFinish(dec(data, iv, alias));
        } catch (InvalidKeyException | UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | IOException e) {
            Log.e("decrypt", "Exception: ", e);
            callback.onFailure();
        }
    }

    /**
     * Decrypts multiple data objects at once. Make absolutely sure the streams are of even length.
     * @see Guard#decryptInternal(String, byte[], String, DecryptedCallback)
     */
    protected synchronized void decryptInternal(@NonNull Stream<String> datas, @NonNull Stream<byte[]> ivs, @NonNull Stream<String> aliases, @NonNull DecryptedCallback callback) {
        Iterator<String> dit = datas.iterator();
        Iterator<byte[]> ivit = ivs.iterator();
        Iterator<String> aliasit = aliases.iterator();

        while (dit.hasNext()) {
            try {
                callback.onFinish(dec(dit.next(), ivit.next(), aliasit.next()));
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
    private synchronized Pair<String, byte[]> enc(@NonNull String data, @NonNull String alias) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
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
    private synchronized String dec(@NonNull String data, @NonNull byte[] iv, @NonNull String alias) throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidAlgorithmParameterException, NoSuchPaddingException, UnrecoverableEntryException, IOException, BadPaddingException, IllegalBlockSizeException {
        if (!this.validated)
            throw new RuntimeException("Cannot perform operation: Not authenticated");
        Cipher cipher = getDecCipher(iv, alias);
        byte[] decrypted = cipher.doFinal(Base64.decode(data, Base64.DEFAULT));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    /**
     * Gets cipher object from Android Keystore, which may be used to encrypt a note
     * @param alias name where key was stored under in Android Keystore
     * @return cipher object, ready for encryption
     */
    private synchronized static @NonNull Cipher getEncCipher(@NonNull String alias)  throws InvalidKeyException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, KeyStoreException, NoSuchPaddingException, IOException {
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
    private synchronized static @NonNull Cipher getDecCipher(@NonNull byte[] iv, @NonNull String alias) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException, IOException {
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
    private synchronized static boolean generateAES(@NonNull String alias) {
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