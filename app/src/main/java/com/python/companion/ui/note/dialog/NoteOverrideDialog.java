package com.python.companion.ui.note.dialog;

import android.content.Context;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.R;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.templates.dialog.DialogAcceptListener;
import com.python.companion.ui.templates.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class NoteOverrideDialog {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private String existsString = "", questionString = "", warningString = "";
        private Note note;
        private Context context;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

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
            return new NoteOverrideDialog(context, dialogCancelListener, dialogAcceptListener, existsString, questionString, warningString, note);
        }
    }

    protected TextView existsView, questionView, warningView;
    protected TextView noteNameView, noteDateView, noteCategoryView;
    protected Button cancelButton, overrideButton;

    protected android.app.Dialog dialog;

    protected Context context;
    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener overrideListener;
    protected String existsText, questionText, warningText;
    protected @NonNull Note note;

    protected NoteOverrideDialog(@NonNull Context context, @Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener overrideListener, String existsText, String questionText, String warningText, @NonNull Note note) {
        this.context = context;
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;
        this.existsText = existsText;
        this.warningText = warningText;
        this.questionText = questionText;
        this.note = note;
    }

    public void showDialog() {
        dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_note_override);

        findGlobalViews();
        setText();
        setNote();
        prepareButtons();
        dialog.show();
    }

    @CallSuper
    protected void findGlobalViews() {
        existsView = dialog.findViewById(R.id.dialog_note_override_text);
        questionView = dialog.findViewById(R.id.dialog_note_override_question);
        warningView = dialog.findViewById(R.id.dialog_note_override_warning);

        noteNameView = dialog.findViewById(R.id.item_note_name);
        noteDateView = dialog.findViewById(R.id.item_note_date);
        noteCategoryView = dialog.findViewById(R.id.item_note_category);

        cancelButton = dialog.findViewById(R.id.dialog_note_override_cancel);
        overrideButton = dialog.findViewById(R.id.dialog_note_override_override);
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
            dialog.dismiss();
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
        });
        overrideButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (overrideListener != null) {
                overrideListener.onAccept();
            }
        });
    }
}