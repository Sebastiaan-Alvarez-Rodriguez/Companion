package com.python.companion.security.converters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.db.entity.Note;
import com.python.companion.security.ExceptionCallback;
import com.python.companion.security.Guard;

public class NoteConverter {

    public static void makeNoteSecure(@NonNull Context context, @NonNull Note note, @NonNull SuccessCallback successCallback) {
        makeNoteSecure(context, note, null, successCallback);
    }

    public static void makeNoteSecure(@NonNull Context context, @NonNull Note note, @Nullable ExceptionCallback exceptionCallback, @NonNull SuccessCallback successCallback) {
        int tmp = Guard.generateAESAndroidKeystore(note.getName());
        if (tmp != Guard.OK) {
            if (exceptionCallback != null)
                exceptionCallback.onException(tmp);
            return;
        }

        Guard.encryptKeystore(note.getContent(), note.getName(), context, (encrypted, iv) -> {
            note.setContent(encrypted);
            note.setIv(iv);
            note.setSecure(true);
            successCallback.onSuccess(note);
        });
    }

    public static void makeNoteInsecure(@NonNull Context context, @NonNull Note note, @NonNull SuccessCallback successCallback) {
        makeNoteInsecure(context, note, null, successCallback);
    }

    //TODO: make use of exceptionCallback
    public static void makeNoteInsecure(@NonNull Context context, @NonNull Note note, @Nullable ExceptionCallback exceptionCallback, @NonNull SuccessCallback successCallback) {
        Guard.decryptKeystore(note.getContent(), note.getIv(), note.getName(), context, plaintext -> {
            note.setContent(plaintext);
            note.setIv(null);
            note.setSecure(false);
            successCallback.onSuccess(note);
        });
    }
}
