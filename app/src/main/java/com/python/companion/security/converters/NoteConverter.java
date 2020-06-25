package com.python.companion.security.converters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.entity.Note;
import com.python.companion.security.DecryptedCallback;
import com.python.companion.security.EncryptedCallback;
import com.python.companion.security.Guard;

public class NoteConverter {
    public static void makeNoteSecure(@NonNull Guard guard, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull Note note, @NonNull ConvertCallback callback) {
        guard.encrypt(note.getContent(), note.getName(), fragmentManager, context, new EncryptedCallback() {
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

    public static void makeNoteInsecure(@NonNull Guard guard, @NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull Note note, @NonNull ConvertCallback callback) {
        guard.decrypt(note.getContent(), note.getIv(), note.getName(), fragmentManager, context, new DecryptedCallback() {
            @Override
            public void onFinish(@NonNull String plaintext) {
                note.setContent("plaintext");
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
}
