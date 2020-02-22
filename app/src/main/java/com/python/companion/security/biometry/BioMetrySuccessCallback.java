package com.python.companion.security.biometry;

import android.hardware.biometrics.BiometricPrompt;

import androidx.annotation.NonNull;

public interface BioMetrySuccessCallback {
    void onSuccess(@NonNull BiometricPrompt.CryptoObject authorizedCryptoObject);
}
