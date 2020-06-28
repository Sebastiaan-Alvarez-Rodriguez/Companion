package com.python.companion.security.password;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

public class PassDialog extends DialogFragment {

    public interface VerifyInterface {
        boolean verify(byte[] password);
    }

//    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener;
        private DialogAcceptValueListener<Boolean> dialogAcceptListener;

        public Builder() {
            dialogCancelListener = null;
            dialogAcceptListener = null;
        }

        public PassDialog.Builder setCancelListener(@NonNull DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public PassDialog.Builder setAcceptListener(@NonNull DialogAcceptValueListener<Boolean> dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }


        public PassDialog build(@NonNull VerifyInterface verifyInterface) {
            if (dialogAcceptListener == null)
                throw new IllegalStateException("Caller must provide accept callback!");
            return new PassDialog(dialogCancelListener, dialogAcceptListener, verifyInterface);
        }
    }

    protected TextView headerView, commentView;
    protected EditText passField;
    protected CheckBox stayLoggedBox;
    protected Button cancelButton, acceptButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @NonNull DialogAcceptValueListener<Boolean> acceptListener;
    protected @NonNull VerifyInterface verifyInterface;

    protected PassDialog(@Nullable DialogCancelListener cancelListener, @NonNull DialogAcceptValueListener<Boolean> acceptListener, @NonNull VerifyInterface verifyInterface) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.verifyInterface = verifyInterface;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_security_pass, container);
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
        headerView = view.findViewById(R.id.dialog_security_pass_text);
        commentView = view.findViewById(R.id.dialog_security_pass_comment);
        passField = view.findViewById(R.id.dialog_security_pass_pass);

        stayLoggedBox = view.findViewById(R.id.dialog_security_pass_staylogged);

        cancelButton = view.findViewById(R.id.dialog_security_pass_cancel);
        acceptButton = view.findViewById(R.id.dialog_security_pass_accept);
    }

    protected void setText() {
        headerView.setText("Security");
        commentView.setText("Stop! Authenticate first.");
        acceptButton.setText("Authenticate");
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });
        acceptButton.setOnClickListener(v -> {
            if (passField.length() == 0) {
                passField.setError("This field cannot be empty");
                return;
            }

            if (verifyInterface.verify(passField.getText().toString().getBytes(Charsets.UTF_8))) {
                acceptListener.onAccept(stayLoggedBox.isChecked());
                dismiss();
            } else {
                commentView.setText("Incorrect password. Try again.");
            }
        });
    }
}
