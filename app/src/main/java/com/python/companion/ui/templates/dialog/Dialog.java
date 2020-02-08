package com.python.companion.ui.templates.dialog;

import android.content.Context;
import android.view.ViewStub;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.python.companion.R;

@SuppressWarnings("WeakerAccess")
public class Dialog {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogOverrideListener dialogOverrideListener = null;

        private String existsString = "", questionString = "", warningString = "";
        private Context context;

        private @LayoutRes int viewLayout = 0;
        private DialogItemInflateListener itemInflateListener = null;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setOverrideListener(DialogOverrideListener dialogOverrideListener) {
            this.dialogOverrideListener = dialogOverrideListener;
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

        public Builder setViewLayout(@LayoutRes int resource) {
            viewLayout = resource;
            return this;
        }

        public Builder setItemInflateListener(DialogItemInflateListener dialogItemInflateListener) {
            itemInflateListener = dialogItemInflateListener;
            return this;
        }

        public Dialog build() {
            return new Dialog(context, dialogCancelListener, dialogOverrideListener, existsString, questionString, warningString, viewLayout, itemInflateListener);
        }
    }

    protected TextView existsView, questionView, warningView;
    protected ViewStub conflictItem;
    protected Button cancelButton, overrideButton;

    protected android.app.Dialog dialog;

    protected Context context;
    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogOverrideListener overrideListener;
    protected @Nullable DialogItemInflateListener itemInflateListener;
    protected String existsText, questionText, warningText;
    protected @LayoutRes int viewLayout;

    protected Dialog(Context context, @Nullable DialogCancelListener cancelListener, @Nullable DialogOverrideListener overrideListener, String existsText, String questionText, String warningText, @LayoutRes int viewLayout, @Nullable DialogItemInflateListener itemInflateListener) {
        this.context = context;
        this.cancelListener = cancelListener;
        this.overrideListener = overrideListener;
        this.existsText = existsText;
        this.warningText = warningText;
        this.questionText = questionText;
        this.viewLayout = viewLayout;
        this.itemInflateListener = itemInflateListener;
    }

    public void showDialog() {
        dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_override);

        findGlobalViews();
        setText();
        setupButtons();
        inflateView();
        dialog.show();
    }

    @CallSuper
    protected void findGlobalViews() {
        existsView = dialog.findViewById(R.id.dialog_conflict_text);
        questionView = dialog.findViewById(R.id.dialog_conflict_question);
        warningView = dialog.findViewById(R.id.dialog_conflict_warning);

        conflictItem = dialog.findViewById(R.id.dialog_conflict_item);
        cancelButton = dialog.findViewById(R.id.dialog_conflict_cancel);
        overrideButton = dialog.findViewById(R.id.dialog_conflict_override);
    }

    protected void setText() {
        existsView.setText(existsText);
        questionView.setText(questionText);
        warningView.setText(warningText);
    }

    protected void inflateView() {
        if (viewLayout != 0 && itemInflateListener != null) {
            conflictItem.setLayoutResource(viewLayout);
            itemInflateListener.onDialogItemInflate(conflictItem.inflate());
        }
    }

    private void setupButtons() {
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
        });
        overrideButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (overrideListener != null) {
                overrideListener.onOverride();
            }
        });
    }
}