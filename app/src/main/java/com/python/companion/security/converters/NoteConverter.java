package com.python.companion.security.converters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.entity.Note;
import com.python.companion.security.Guard;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Convenient class to convert notes between plaintext and ciphertext
 */
public class NoteConverter {

    /**
     * Encrypt a note
     * @param note Note to encrypt
     * @param callback Receives update on success or failure
     */
    public static void noteEncrypt(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull Note note, @NonNull ConvertCallback callback) {
        Guard.getGuard().encrypt(note.getContent(), note.getName(), fragmentManager, context, new Guard.EncryptedCallback() {
            @Override
            public void onFinish(@NonNull String encrypted, @NonNull byte[] iv) {
                note.setContent(encrypted);
                note.setIv(iv);
                note.setSecure(true);
                callback.onSuccess(note);
            }

            @Override
            public void onFailure() {
                callback.onFailure();
            }
        });
    }

    /**
     * Decrypt a note
     * @param note Note to decrypt
     * @param callback Receives update on success or failure
     */
    public static void noteDecrypt(@NonNull FragmentManager manager, @NonNull Context context, @NonNull Note note, @NonNull ConvertCallback callback) {
        Guard.getGuard().decrypt(note.getContent(), note.getIv(), note.getName(), manager, context, new Guard.DecryptedCallback() {
            @Override
            public void onFinish(@NonNull String plaintext) {
                note.setContent(plaintext);
                note.setIv(null);
                note.setSecure(false);
                callback.onSuccess(note);
            }

            @Override
            public void onFailure() {
                callback.onFailure();
            }
        });
    }

    /**
     * Decrypts a batch of notes at once
     * @param notes Notes to decrypt
     * @param callback Receives {@code notes.size()} calls to indicate success or failure for individual notes
     */
    public static void batchDecrypt(@NonNull FragmentManager manager, @NonNull Context context, @NonNull List<Note> notes, @NonNull ConvertCallback callback) {
        Stream<String> datas = notes.stream().map(Note::getContent);
        Stream<byte[]> ivs = notes.stream().map(Note::getIv);
        Stream<String> aliases = notes.stream().map(Note::getName);

        Iterator<Note> outit = notes.stream().iterator();
        Guard.getGuard().decrypt(datas, ivs, aliases, manager, context, new Guard.DecryptedCallback() {
            @Override
            public void onFinish(@NonNull String plaintext) {
                Note n = outit.next();
                n.setContent(plaintext);
                n.setIv(null);
                n.setSecure(false);
                callback.onSuccess(n);
            }
            @Override
            public void onFailure() {
                callback.onFailure();
            }
        });
    }

    /**
     * Encrypts a batch of notes at once
     * @param notes Notes to encrypt
     * @param callback Receives {@code notes.size()} calls to indicate success or failure for individual notes
     */
    public static void batchEncrypt(@NonNull FragmentManager manager, @NonNull Context context, @NonNull List<Note> notes, @NonNull ConvertCallback callback) {
        Stream<String> datas = notes.stream().map(Note::getContent);
        Stream<String> aliases = notes.stream().map(Note::getName);

        Iterator<Note> outit = notes.stream().iterator();
        Guard.getGuard().encrypt(datas, aliases, manager, context, new Guard.EncryptedCallback() {
            @Override
            public void onFinish(@NonNull String encrypted, @NonNull byte[] iv) {
                Note n = outit.next();
                n.setContent(encrypted);
                n.setIv(iv);
                n.setSecure(true);
                callback.onSuccess(n);
            }
            @Override
            public void onFailure() { callback.onFailure(); }
        });
    }

    /** Callback used to receive conversion result */
    public interface ConvertCallback {
        void onSuccess(@NonNull Note note);
        void onFailure();
    }
}
