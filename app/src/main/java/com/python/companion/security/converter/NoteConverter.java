package com.python.companion.security.converter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Note;
import com.python.companion.security.ExceptionCallback;
import com.python.companion.security.Guard;

public class NoteConverter {

    public static void makeNoteSecure(@NonNull Context context, @NonNull Note note) {
        makeNoteSecure(context, note, null, null);
    }

    public static void makeNoteSecure(@NonNull Context context, @NonNull Note note, @Nullable ExceptionCallback exceptionCallback, @Nullable SuccessCallback successCallback) {
        int tmp = Guard.generatePasswordBasedAESAndroidKeystore(note.getName());
        if (tmp != Guard.OK) {
            if (exceptionCallback != null)
                exceptionCallback.onException(tmp);
            return;
        }

        Guard.encryptKeystore(note.getContent(), note.getName(), context, (encrypted, iv) -> {
            note.setContent(encrypted);
            note.setIv(iv);
            note.setSecure(true);
            NoteQuery noteQuery = new NoteQuery(context);
            noteQuery.update(note);
            if (successCallback != null)
                successCallback.onSuccess();
        });
    }

    public static void makeNoteInsecure(@NonNull Context context, @NonNull Note note) {
        makeNoteInsecure(context, note, null, null);
    }

    public static void makeNoteInsecure(@NonNull Context context, @NonNull Note note, @Nullable ExceptionCallback exceptionCallback, @Nullable SuccessCallback successCallback) {
        Guard.decryptKeystore(note.getContent(), note.getIv(), note.getName(), context, plaintext -> {
            note.setContent(plaintext);
            note.setIv(null);
            note.setSecure(false);
            NoteQuery noteQuery = new NoteQuery(context);
            noteQuery.update(note);
            if (successCallback != null)
                successCallback.onSuccess();
        });
    }
}
