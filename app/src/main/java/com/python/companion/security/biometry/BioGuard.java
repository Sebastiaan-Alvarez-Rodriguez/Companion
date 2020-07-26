package com.python.companion.security.biometry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.security.Guard;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.FinishListener;

public class BioGuard extends Guard {
    @Override
    protected void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        Biometry bio = new Biometry.Builder()
                .setSuccessCallback(finishListener::onFinish)
                .setCancelCallback(() -> errorListener.onError("User cancelled authentication"))
                .setFailureCallback(() -> errorListener.onError("Biometric not recognized"))
                .setErrorCallback(errorListener::onError)
                .setHelpCallback(helpString -> errorListener.onError("Help: "+helpString))
                .build(context);
        bio.authorize();
    }
}