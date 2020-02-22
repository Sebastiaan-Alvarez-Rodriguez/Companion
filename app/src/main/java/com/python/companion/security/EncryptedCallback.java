package com.python.companion.security;

import androidx.annotation.NonNull;

public interface EncryptedCallback {
    void onFinish(@NonNull String encrypted, @NonNull String iv);
}
