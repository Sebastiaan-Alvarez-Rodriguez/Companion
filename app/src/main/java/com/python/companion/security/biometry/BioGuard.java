package com.python.companion.security.biometry;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.security.Guard;
import com.python.companion.security.ValidateCallback;
import com.python.companion.security.biometry.callbacks.BioMetryCancelCallback;
import com.python.companion.security.biometry.callbacks.BioMetryErrorCallback;
import com.python.companion.security.biometry.callbacks.BioMetryFailureCallback;
import com.python.companion.security.biometry.callbacks.BioMetryHelpCallback;

public class BioGuard extends Guard {
    @Override
    protected void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull ValidateCallback validateCallback) {
        Biometry bio = new Biometry.Builder()
                .setSuccessCallback(validateCallback::onSuccess)
                .setCancelCallback(new BioMetryCancelCallback() {
                    @Override
                    public void onCancel() {
                        Log.e("Bio", "user cancelled");
                        validateCallback.onFailure();
                    }
                })
                .setFailureCallback(new BioMetryFailureCallback() {
                    @Override
                    public void onFailure() {
                        Log.e("Bio", "failure!");
                        validateCallback.onFailure();
                    }
                })
                .setErrorCallback(new BioMetryErrorCallback() {
                    @Override
                    public void onError(String errorString) {
                        Log.e("Bio", "Error! Errorstring: "+errorString);
                        validateCallback.onFailure();
                    }
                })
                .setHelpCallback(new BioMetryHelpCallback() {
                    @Override
                    public void onHelpNeeded(String helpString) {
                        Log.e("Bio", "Help needed. Helpstring: "+helpString);
                        validateCallback.onFailure();
                    }
                })
                .build(context);
        bio.authorize();
    }
}