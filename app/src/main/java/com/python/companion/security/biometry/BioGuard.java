package com.python.companion.security.biometry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.security.Guard;
import com.python.companion.security.ValidateCallback;

public class BioGuard extends Guard {
    @Override
    protected void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull ValidateCallback validateCallback) {
        Biometry bio = new Biometry.Builder()
                .setSuccessCallback(validateCallback::onSuccess)
                .setCancelCallback(validateCallback::onFailure)
                .setFailureCallback(validateCallback::onFailure)
                .setErrorCallback(errorString -> validateCallback.onFailure())
                .setHelpCallback(helpString -> validateCallback.onFailure())
                .build(context);
        bio.authorize();
    }
}