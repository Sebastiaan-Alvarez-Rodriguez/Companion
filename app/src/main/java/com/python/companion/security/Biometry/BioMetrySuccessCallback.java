package com.python.companion.security.Biometry;

import android.hardware.biometrics.BiometricPrompt;

import androidx.annotation.NonNull;

public interface BioMetrySuccessCallback {
    void onSuccess(@NonNull BiometricPrompt.CryptoObject authorizedCryptoObject);
}
