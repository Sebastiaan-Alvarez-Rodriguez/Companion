package com.python.companion.security.converters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.entity.Note;
import com.python.companion.security.DecryptedCallback;
import com.python.companion.security.EncryptedCallback;
import com.python.companion.security.Guard;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class NoteConverter {
    public static void makeNoteSecure(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull Note note, @NonNull ConvertCallback callback) {
        Guard.getGuard().encrypt(note.getContent(), note.getName(), fragmentManager, context, new EncryptedCallback() {
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

    public static void makeNoteInsecure(@NonNull FragmentManager manager, @NonNull Context context, @NonNull Note note, @NonNull ConvertCallback callback) {
        Guard.getGuard().decrypt(note.getContent(), note.getIv(), note.getName(), manager, context, new DecryptedCallback() {
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

    public static void batchInsecure(@NonNull FragmentManager manager, @NonNull Context context, @NonNull List<Note> notes, @NonNull ConvertCallback callback) {
        Stream<String> datas = notes.stream().map(Note::getContent);
        Stream<byte[]> ivs = notes.stream().map(Note::getIv);
        Stream<String> aliases = notes.stream().map(Note::getName);

        Iterator<Note> outit = notes.stream().iterator();
        Guard.getGuard().decrypt(datas, ivs, aliases, manager, context, new DecryptedCallback() {
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

    public static void batchSecure(@NonNull FragmentManager manager, @NonNull Context context, @NonNull List<Note> notes, @NonNull ConvertCallback callback) {
        Stream<String> datas = notes.stream().map(Note::getContent);
        Stream<String> aliases = notes.stream().map(Note::getName);

        Iterator<Note> outit = notes.stream().iterator();
        Guard.getGuard().encrypt(datas, aliases, manager, context, new EncryptedCallback() {
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
}
