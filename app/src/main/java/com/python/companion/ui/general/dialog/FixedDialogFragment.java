package com.python.companion.ui.general.dialog;

import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.fragment.app.DialogFragment;

/**
 * This {@link DialogFragment} implementation fixes a very annoying layout bug
 */
public abstract class FixedDialogFragment extends DialogFragment {

    @Override
    @CallSuper
    public void onResume() {
        rescale();
        super.onResume();
    }

    /**
     * Method to fix Android's incorrect implementation of 'match_parent'.
     * Calling this method actually sets window parameters correctly and redraws our window
     */
    private void rescale() {
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes(params);
    }
}
