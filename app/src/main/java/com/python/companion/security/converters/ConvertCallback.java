package com.python.companion.security.converters;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Note;

public interface ConvertCallback {
    void onSuccess(@NonNull Note note);
    void onFailure();
}
