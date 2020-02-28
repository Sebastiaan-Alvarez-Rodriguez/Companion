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
import com.python.companion.ui.templates.dialog.DialogAcceptListener;
import com.python.companion.ui.templates.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class NoteOverrideDialog extends DialogFragment {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private String existsString = "", questionString = "", warningString = "";
        private Note note;
        
        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setOverrideListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setExistsText(String text) {
            this.existsString = text;
            return this;
        }

        public Builder setQuestionText(String text) {
            this.questionString = text;
            return this;
        }

        public Builder setWarningText(String text) {
            this.warningString = text;
            return this;
        }

        public Builder setNote(Note note) {
            this.note = note;
            return this;
        }

        public NoteOverrideDialog build() {
            if (note == null)
                throw new IllegalStateException("Caller must provide Note which will be overriden with builder.setNote(note)");
            return new NoteOverrideDialog(dialogCancelListener, dialogAcceptListener, existsString, questionString, warningString, note);
        }
    }

    protected TextView existsView, questionView, warningView;
    protected TextView noteNameView, noteDateView, noteCategoryView;
    protected Button cancelButton, overrideButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener overrideListener;
    protected String existsText, questionText, warningText;
    protected @NonNull Note note;

    protected NoteOverrideDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener overrideListener, String existsText, String questionText, String warningText, @NonNull Note note) {
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;
        this.existsText = existsText;
        this.warningText = warningText;
        this.questionText = questionText;
        this.note = note;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_note_override, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setText();
        setNote();
        prepareButtons();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        existsView = view.findViewById(R.id.dialog_note_override_text);
        questionView = view.findViewById(R.id.dialog_note_override_question);
        warningView = view.findViewById(R.id.dialog_note_override_warning);

        noteNameView = view.findViewById(R.id.item_note_name);
        noteDateView = view.findViewById(R.id.item_note_date);
        noteCategoryView = view.findViewById(R.id.item_note_category);

        cancelButton = view.findViewById(R.id.dialog_note_override_cancel);
        overrideButton = view.findViewById(R.id.dialog_note_override_override);
    }

    protected void setText() {
        existsView.setText(existsText);
        questionView.setText(questionText);
        warningView.setText(warningText);
    }

    protected void setNote() {
        noteNameView.setText(note.getName());
        noteDateView.setText(note.getModified().toString());
        noteCategoryView.setBackgroundColor(note.getCategory().getCategoryColor());
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        overrideButton.setOnClickListener(v -> {
            if (overrideListener != null) {
                overrideListener.onAccept();
            }
            dismiss();
        });
    }
}