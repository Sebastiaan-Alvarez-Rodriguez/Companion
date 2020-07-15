package com.python.companion.ui.notes.note.dialog;

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
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.ui.general.dialog.DialogAcceptValueListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;

public class LockDialog extends DialogFragment {

    public static class Builder {
        private DialogCancelListener dialogCancelListener;
        private DialogAcceptValueListener<Note> dialogAcceptListener;
        
        private Note note;

        public Builder() {
            dialogCancelListener = null;
            dialogAcceptListener = null;
            note = null;
        }

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

        public LockDialog build(boolean lock) {
            if (note == null)
                throw new IllegalStateException("Caller must provide Note which will be locked/unlocked with builder.setNote(note)");
            else if (dialogAcceptListener == null)
                throw new IllegalStateException("Caller must provide accept callback!");
            return new LockDialog(dialogCancelListener, dialogAcceptListener, note, lock);
        }
    }

    protected TextView titleView, questionView, warningView;
    protected Button cancelButton, acceptButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @NonNull DialogAcceptValueListener<Note> acceptListener;
    protected @NonNull Note note;
    protected boolean lock;

    protected LockDialog(@Nullable DialogCancelListener cancelListener, @NonNull DialogAcceptValueListener<Note> acceptListener, @NonNull Note note, boolean lock) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.note = note;
        this.lock = lock;
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
        titleView.setText(lock ? "Lock" : "Unlock");
        questionView.setText("Do you want to "+ (lock ? "lock" : "unlock") + " this note?");
        warningView.setText(lock ? "Locking this note means that you have to authenticate before viewing it" : "Warning: No authentication is required to view unlocked notes");
        acceptButton.setText(lock ? "Lock" : "Unlock");
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });
        acceptButton.setOnClickListener(v -> {
            if (lock) {
                NoteConverter.noteEncrypt(getChildFragmentManager(), getContext(), note, new NoteConverter.ConvertCallback() {
                    @Override
                    public void onSuccess(@NonNull Note note) {
                        acceptListener.onAccept(note);
                        dismiss();
                    }
                    @Override
                    public void onFailure() {}
                });
            } else {
                NoteConverter.noteDecrypt(getChildFragmentManager(), getContext(), note, new NoteConverter.ConvertCallback() {
                    @Override
                    public void onSuccess(@NonNull Note note) {
                        acceptListener.onAccept(note);
                        dismiss();
                    }
                    @Override
                    public void onFailure() {}
                });
            }
        });
    }
}
