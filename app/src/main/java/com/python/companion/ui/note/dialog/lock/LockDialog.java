package com.python.companion.ui.note.dialog.lock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.python.companion.R;
import com.python.companion.db.entity.Note;
import com.python.companion.security.Guard;
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.ui.note.dialog.ErrorDialog;
import com.python.companion.ui.templates.dialog.DialogAcceptValueListener;
import com.python.companion.ui.templates.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class LockDialog extends DialogFragment {
    
    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptValueListener<Note> dialogAcceptListener = null;
        
        private Note note;

        public Builder setCancelListener(@NonNull DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setAcceptListener(@NonNull DialogAcceptValueListener<Note> dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setNote(Note note) {
            this.note = note;
            return this;
        }

        public LockDialog build() {
            if (note == null)
                throw new IllegalStateException("Caller must provide Note which will be locked/unlocked with builder.setNote(note)");
            else if (dialogAcceptListener == null)
                throw new IllegalStateException("Caller must provide accept callback!");
            return new LockDialog(dialogCancelListener, dialogAcceptListener, note);
        }
    }

    protected TextView titleView, questionView, warningView;
    protected Button cancelButton, acceptButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @NonNull DialogAcceptValueListener<Note> acceptListener;
    protected @NonNull Note note;

    protected LockDialog(@Nullable DialogCancelListener cancelListener, @NonNull DialogAcceptValueListener<Note> acceptListener, @NonNull Note note) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.note = note;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_note_lock, container);
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
        titleView = view.findViewById(R.id.dialog_note_lock_text);
        questionView = view.findViewById(R.id.dialog_note_lock_question);
        warningView = view.findViewById(R.id.dialog_note_lock_warning);

        cancelButton = view.findViewById(R.id.dialog_note_lock_cancel);
        acceptButton = view.findViewById(R.id.dialog_note_lock_accept);
    }

    protected void setText() {
        boolean secure = note.isSecure();
        titleView.setText(secure ? "Unlock" : "Lock");
        questionView.setText("Do you want to "+ (secure ? "unlock" : "lock") + " this note?");
        if (secure)
            warningView.setText("Warning: No authentication is required to view unlocked notes");
        else
            warningView.setText("Locking this note means that you have to authenticate before viewing it");
        acceptButton.setText(secure ? "Unlock" : "Lock");
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });
        acceptButton.setOnClickListener(v -> {
            if (note.isSecure())
                NoteConverter.makeNoteInsecure(getContext(), note, exception -> {
                }, note -> {
                    acceptListener.onAccept(note);
                    dismiss();
                });
            else
                NoteConverter.makeNoteSecure(getContext(), note, exception -> {
                    ErrorDialog errorDialog;
                    switch (exception) {
                        case Guard.NO_BIOMETRICS:
                            errorDialog = new ErrorDialog.Builder()
                                    .setSubtitle("No biometrics registered")
                                    .setProblem("Problem: You have not enrolled any fingerprints, which is required to lock notes")
                                    .setSolution("Solution: Go to Settings > Security > Fingerprint, and enroll a fingerprint")
                                    .build();
                            break;
                        case Guard.OTHER_PROBLEM:
                        default:
                            errorDialog = new ErrorDialog.Builder()
                                    .setSubtitle("An unknown error occured")
                                    .setProblem("We do not know what happened. Please remember this error code: <"+exception+">. Let us know you encoutered it, and what you did with this note")
                                    .build();
                            break;
                    }
                    errorDialog.show(getChildFragmentManager(), null);
                }, note -> {
                    acceptListener.onAccept(note);
                    dismiss();
                });
        });
    }
}
