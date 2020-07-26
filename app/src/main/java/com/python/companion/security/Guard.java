package com.python.companion.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.R;
import com.python.companion.db.entity.Note;
import com.python.companion.security.biometry.BioGuard;
import com.python.companion.security.password.PassGuard;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.FinishListener;
import com.python.companion.util.genericinterfaces.ResultListener;

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

    protected abstract void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener);

    /**
     * Public call to handle encryption once
     * @see Guard#encryptInternal(Note, EncryptedCallback)
     * @param note Note to encrypt
     * @param callback Called when encryption had success
     * @param errorListener Called when fatal error occurred
     */
    public synchronized void encrypt(@NonNull Note note, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull EncryptedCallback callback, @NonNull ErrorListener errorListener) {
        if (note.isSecure()) {
            errorListener.onError("Note already is secure");
            return;
        }

        if (note.getIv() != null) {
            errorListener.onError("Note already has iv");
            return;
        }

        if (!this.validated) {
            validate(fragmentManager, context, () -> {
                synchronized (Guard.class) {
                    final boolean wasvalidated = Guard.this.validated;
                    Guard.this.validated = true;
                    encryptInternal(note, callback);
                    if (!wasvalidated)
                        Guard.this.validated = false;
                }
            }, errorListener);
        } else {
            encryptInternal(note, callback);
        }
    }

    /**
     * Public call to handle encryption of multiple items at once. For each item, EncryptedCallback will be called.
     * On fatal error, the ErrorCallback will be called
     * On finishing encryption, the finishListener will be called with all successfully processed Notes in a stream
     * @see Guard#encrypt(Note, FragmentManager, Context, EncryptedCallback, ErrorListener)
     */
    public synchronized void encrypt(@NonNull Stream<Note> notes, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull EncryptedCallback callback, @NonNull ResultListener<Stream<Note>> finishListener, @NonNull ErrorListener errorListener) {
        if (!this.validated) {
            validate(fragmentManager, context, () -> {
                synchronized (Guard.class) {
                    final boolean wasvalidated = Guard.this.validated;
                    Guard.this.validated = true;
                    encryptInternal(notes, callback, finishListener, errorListener);
                    if (!wasvalidated)
                        Guard.this.validated = false;
                }
            }, errorListener);
        } else {
            encryptInternal(notes, callback, finishListener, errorListener);
        }
    }

    /**
     * Public call to handle decryption once
     * @see Guard#decryptInternal(Note, DecryptedCallback)
     * @param note Note with encrypted data and set iv
     * @param callback Called when decryption had success
    * @param errorListener Called when fatal error occurred
     */
    public synchronized void decrypt(@NonNull Note note, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull DecryptedCallback callback,  @NonNull ErrorListener errorListener) {
        if (note.getIv() == null) {
            errorListener.onError("Note has no iv set");
            return;
        }
        if (!note.isSecure()) {
            errorListener.onError("Note is not secure");
            return;
        }

        if (!this.validated) {
            validate(fragmentManager, context, () -> {
                    synchronized (Guard.class) {
                        final boolean wasvalidated = Guard.this.validated;
                        Guard.this.validated = true;
                        decryptInternal(note, callback);
                        if (!wasvalidated)
                            Guard.this.validated = false;
                    }
                }, errorListener);
        } else {
            decryptInternal(note, callback);
        }
    }

    /**
     * Public call to handle decryption of multiple items at once. For each item, the callback will be called
     * @see Guard#decrypt(Note, FragmentManager, Context, DecryptedCallback, ErrorListener)
     */
    public synchronized void decrypt(@NonNull Stream<Note> notes, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull DecryptedCallback callback, @NonNull ResultListener<Stream<Note>> finishListener, @NonNull ErrorListener errorListener) {
        if (!this.validated) {
            validate(fragmentManager, context, () -> {
                    synchronized (Guard.class) {
                        final boolean wasvalidated = Guard.this.validated;
                        Guard.this.validated = true;
                        // This would be a major security vulnerability, since a thread could be used to boot other functions while this thread is busy.
                        // Luckily, every method is synchronized, meaning we have a lock, and only 1 method can be active at any point in time.
                        // When control reaches this statement, we own the lock. No abuse is possible (In this manner)
                        decryptInternal(notes, callback, finishListener);
                        if (!wasvalidated)
                            Guard.this.validated = false;
                    }
                }, errorListener);
        } else {
            decryptInternal(notes, callback, finishListener);
        }
    }

    /**
     * Encrypt given string using key in Android KeyStore with given alias, and return content in a {@link EncryptedCallback}
     */
    private synchronized void encryptInternal(@NonNull Note note, @NonNull EncryptedCallback callback) {
        try {
            EncryptionTuple tuple = enc(note.getContent(), note.getName());
            note.setContent(tuple.ciphertext);
            note.setIv(tuple.iv);
            note.setSecure(true);
            callback.onEncrypted(note);
        } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | KeyStoreException | NoSuchPaddingException | IOException | BadPaddingException | IllegalBlockSizeException e) {
            Log.e("encrypt", "Exception: ", e);
            callback.onEncryptFailure();
        }
    }

    /**
     * Encrypts multiple data objects at once. Make absolutely sure the streams are of even length.
     * @see Guard#encryptInternal(Note, EncryptedCallback)
     */
    @SuppressWarnings({"unused"})
    protected synchronized void encryptInternal(@NonNull Stream<Note> notes, @NonNull EncryptedCallback callback, @NonNull ResultListener<Stream<Note>> finishListener, @NonNull ErrorListener errorListener) {
        Iterator<Note> noteIt = notes.iterator();

        Stream.Builder<Note> stream = Stream.builder();
        while (noteIt.hasNext()) {
            try {
                Note n = noteIt.next();
                EncryptionTuple tuple = enc(n.getContent(), n.getName());

                n.setContent(tuple.ciphertext);
                n.setIv(tuple.iv);
                n.setSecure(true);

                callback.onEncrypted(n);
                stream.accept(n);
            } catch (InvalidKeyException | UnrecoverableEntryException | NoSuchAlgorithmException | CertificateException | KeyStoreException | NoSuchPaddingException | IOException | BadPaddingException | IllegalBlockSizeException e) {
                Log.e("encrypt", "Exception: ", e);
                callback.onEncryptFailure();
            }
        }
        finishListener.onResult(stream.build());
    }

    /**
     * Decrypt given String using key in Android KeyStore with given alias, and return content in a {@link DecryptedCallback}
     */
    @SuppressWarnings("ConstantConditions")
    protected synchronized void decryptInternal(@NonNull Note note, @NonNull DecryptedCallback callback) {
        try {
            String plaintext = dec(note.getContent(), note.getIv(), note.getName());

            note.setContent(plaintext);
            note.setIv(null);
            note.setSecure(false);

            callback.onDecrypted(note);
        } catch (InvalidKeyException | UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | IOException e) {
            Log.e("decrypt", "Exception: ", e);
            callback.onDecryptFailure();
        }
    }

    /**
     * Decrypts multiple data objects at once. Make absolutely sure the streams are of even length.
     * @see Guard#decryptInternal(Note, DecryptedCallback)
     */
    protected synchronized void decryptInternal(@NonNull Stream<Note> notes, @NonNull DecryptedCallback callback, @NonNull ResultListener<Stream<Note>> finishListener) {
        Iterator<Note> noteIt = notes.iterator();

        Stream.Builder<Note> stream = Stream.builder();

        while (noteIt.hasNext()) {
            Note note = noteIt.next();
            try {
                String plain = dec(note.getContent(), note.getIv(), note.getName());

                note.setContent(plain);
                note.setIv(null);
                note.setSecure(false);

                callback.onDecrypted(note);
                stream.accept(note);
            } catch (InvalidKeyException | UnrecoverableEntryException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | IOException e) {
                Log.e("decrypt", "Exception: ", e);
                callback.onDecryptFailure();
            }
        }
        finishListener.onResult(stream.build());
    }
    
    /**
     * Encrypt given string using key in Android KeyStore with given alias, and return content in a {@link EncryptedCallback}
     * @return Pair of encrypted string, iv
     */
    private synchronized EncryptionTuple enc(@NonNull String data, @NonNull String alias) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException {
        if (!this.validated)
            throw new RuntimeException("Cannot perform operation: Not authenticated");
        Cipher cipher = getEncCipher(alias);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return new EncryptionTuple(Base64.encodeToString(encrypted, Base64.DEFAULT), iv);
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


    /** Interface for receiving decryption callbacks */
    public interface DecryptedCallback {
        /** Called when decryption was successful, with generated plaintext set, and iv set to null */
        void onDecrypted(@NonNull Note note);
        /** Called when decryption fails */
        void onDecryptFailure();
    }

    /** Interface for receiving encryption callbacks */
    public interface EncryptedCallback {
        /** Called when encryption was successful, with generated ciphertext and securely random generated initialization vector (IV) set */
        void onEncrypted(@NonNull Note note);
        /** Called when encryption fails */
        void onEncryptFailure();
    }
}