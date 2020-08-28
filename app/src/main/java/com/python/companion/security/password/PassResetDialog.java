package com.python.companion.security.password;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.R;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.python.companion.ui.general.dialog.FixedDialogFragment;

@SuppressWarnings("WeakerAccess")
public class PassResetDialog extends FixedDialogFragment {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setAcceptListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public PassResetDialog build() {
            return new PassResetDialog(dialogCancelListener, dialogAcceptListener);
        }
    }

    protected Button cancelButton, acceptButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener overrideListener;

    protected PassResetDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener overrideListener) {
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_security_pass_reset, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        prepareButtons();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        cancelButton = view.findViewById(R.id.dialog_security_pass_reset_cancel);
        acceptButton = view.findViewById(R.id.dialog_security_pass_reset_accept);
    }


    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        acceptButton.setOnClickListener(v -> {
            if (overrideListener != null) {
                overrideListener.onAccept();
            }
            dismiss();
        });
    }
}