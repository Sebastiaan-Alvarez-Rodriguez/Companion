package com.python.companion.security.biometry;

import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.security.biometry.callbacks.BioMetryCancelCallback;
import com.python.companion.security.biometry.callbacks.BioMetryErrorCallback;
import com.python.companion.security.biometry.callbacks.BioMetryFailureCallback;
import com.python.companion.security.biometry.callbacks.BioMetryHelpCallback;
import com.python.companion.security.biometry.callbacks.BioMetrySuccessCallback;

import java.util.concurrent.Executors;

public class Biometry {
    protected Context context;
    protected @Nullable
    BioMetryCancelCallback cancelCallback;
    protected @Nullable
    BioMetryErrorCallback errorCallback;
    protected @Nullable
    BioMetryFailureCallback failureCallback;
    protected @Nullable
    BioMetryHelpCallback helpCallback;
    protected @NonNull
    BioMetrySuccessCallback successCallback;

    public static class Builder {
        protected BioMetryCancelCallback cancelCallback;

        protected BioMetryErrorCallback errorCallback;
        protected BioMetryFailureCallback failureCallback;
        protected BioMetryHelpCallback helpCallback;
        protected BioMetrySuccessCallback successCallback;



        public Builder setCancelCallback(@NonNull BioMetryCancelCallback cancelCallback) {
            this.cancelCallback = cancelCallback;
            return this;
        }

        public Builder setErrorCallback(@NonNull BioMetryErrorCallback errorCallback) {
            this.errorCallback = errorCallback;
            return this;
        }
        public Builder setFailureCallback(@NonNull BioMetryFailureCallback failureCallback) {
            this.failureCallback = failureCallback;
            return this;
        }
        public Builder setHelpCallback(@NonNull BioMetryHelpCallback helpCallback) {
            this.helpCallback = helpCallback;
            return this;
        }
        public Builder setSuccessCallback(@NonNull BioMetrySuccessCallback successCallback) {
            this.successCallback = successCallback;
            return this;
        }

        @CheckResult
        public Biometry build(@NonNull Context context) {
            return new Biometry(context, cancelCallback, errorCallback, failureCallback, helpCallback, successCallback);
        }
    }

    protected Biometry(@NonNull Context context,  @Nullable BioMetryCancelCallback cancelCallback, @Nullable BioMetryErrorCallback errorCallback, @Nullable BioMetryFailureCallback failureCallback, @Nullable BioMetryHelpCallback helpCallback, @NonNull BioMetrySuccessCallback successCallback) {
        this.context = context;
        this.cancelCallback = cancelCallback;
        this.errorCallback = errorCallback;
        this.failureCallback = failureCallback;
        this.helpCallback = helpCallback;
        this.successCallback = successCallback;
    }

    /** Function to begin authorization of user, with default title, description, and subtitle */
    public void authorize() {
        BiometricPrompt bio = new BiometricPrompt.Builder(context)
                .setTitle("Guard")
                .setDescription("Please login to continue")
                .setSubtitle("Authorization required")
                .setNegativeButton("Cancel", Executors.newSingleThreadExecutor(), (dialog, which) -> {
                    if (cancelCallback != null)
                        cancelCallback.onCancel();
                }).build();
        bio.authenticate(new CancellationSignal(), Executors.newSingleThreadExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (errorCallback != null)
                    errorCallback.onError(errString.toString());
            }

            @Override
            public void onAuthenticationFailed() {
                if (failureCallback != null)
                    failureCallback.onFailure();
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                if (helpCallback != null)
                    helpCallback.onHelpNeeded(helpString.toString());
            }


            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                successCallback.onSuccess();
            }
        });
    }
}
