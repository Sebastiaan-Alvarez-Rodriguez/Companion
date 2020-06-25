package com.python.companion.security.password;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.python.companion.R;
import com.python.companion.ui.general.dialog.DialogAcceptValueListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;

import kotlin.text.Charsets;

public class PassSetDialog extends DialogFragment {

//    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener;
        private DialogAcceptValueListener<byte[]> dialogAcceptListener;

        public Builder() {
            dialogCancelListener = null;
            dialogAcceptListener = null;
        }

        public PassSetDialog.Builder setCancelListener(@NonNull DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public PassSetDialog.Builder setAcceptListener(@NonNull DialogAcceptValueListener<byte[]> dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }


        public PassSetDialog build(@NonNull PassGuard guard) {
            if (dialogAcceptListener == null)
                throw new IllegalStateException("Caller must provide accept callback!");
            return new PassSetDialog(dialogCancelListener, dialogAcceptListener, guard);
        }
    }

    protected TextView headerView, textView;
    protected EditText passOneField, passTwoField;
    protected Button cancelButton, acceptButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @NonNull DialogAcceptValueListener<byte[]> acceptListener;
    protected @NonNull PassGuard passGuard;

    protected PassSetDialog(@Nullable DialogCancelListener cancelListener, @NonNull DialogAcceptValueListener<byte[]> acceptListener, @NonNull PassGuard guard) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.passGuard = guard;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_security_pass_set, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setText();
        prepareButtons();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        headerView = view.findViewById(R.id.dialog_security_pass_set_header);
        textView = view.findViewById(R.id.dialog_security_pass_set_comment);
        passOneField = view.findViewById(R.id.dialog_security_pass_set_pass_one);
        passTwoField = view.findViewById(R.id.dialog_security_pass_set_pass_two);

        cancelButton = view.findViewById(R.id.dialog_security_pass_set_cancel);
        acceptButton = view.findViewById(R.id.dialog_security_pass_set_accept);
    }

    protected void setText() {
        headerView.setText("Security");
        textView.setText("Stop! Authenticate first.");
        acceptButton.setText("Authenticate");
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });
        acceptButton.setOnClickListener(v -> {
            if (passOneField.length() < 12) {
                passOneField.setError("Password must be at least 12 characters long, preferably more");
                return;
            }

            if (!passOneField.getText().toString().equals(passTwoField.getText().toString())) {
                passTwoField.setError("Passwords do not match!");
                return;
            }
            acceptListener.onAccept(passOneField.getText().toString().getBytes(Charsets.UTF_8));
            dismiss();
        });
    }
}
