package com.python.companion.security;

import androidx.annotation.NonNull;

public interface DecryptedCallback {
    void onFinish(@NonNull String plaintext);
    void onFailure();
}
