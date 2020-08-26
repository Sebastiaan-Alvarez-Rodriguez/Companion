package com.python.companion.ui.anniversary.dialog;

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
import com.python.companion.db.interact.AnniversaryStore;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;

public class AnniversaryDeleteDialog extends DialogFragment {

    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private String existsString = "", questionString = "", warningString = "";
        private AnniversaryWithParentNames anniversaryWithParentName;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setDeleteListener(DialogAcceptListener dialogAcceptListener) {
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

        public Builder setAnniversaryWithParentName(AnniversaryWithParentNames anniversaryWithParentNames) {
            this.anniversaryWithParentName = anniversaryWithParentNames;
            return this;
        }

        public AnniversaryDeleteDialog build() {
            if (anniversaryWithParentName == null)
                throw new IllegalStateException("Caller must provide Anniversary which will be deleted with builder.setAnniversary(anniversary)");
            return new AnniversaryDeleteDialog(dialogCancelListener, dialogAcceptListener, existsString, questionString, warningString, anniversaryWithParentName);
        }
    }

    protected TextView existsView, questionView, warningView;
    protected TextView anniversaryNameView, anniversaryEqualityView, anniversaryDefinitionView;
    protected Button cancelButton, overrideButton;

    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener deleteListener;
    protected String existsText, questionText, warningText;
    protected @NonNull AnniversaryWithParentNames anniversaryWithParentNames;

    protected AnniversaryDeleteDialog(@Nullable DialogCancelListener cancelListener, @NonNull DialogAcceptListener deleteListener, String existsText, String questionText, String warningText, @NonNull AnniversaryWithParentNames anniversaryWithParentNames) {
        this.cancelListener = cancelListener;
        this.deleteListener = deleteListener;
        this.existsText = existsText;
        this.warningText = warningText;
        this.questionText = questionText;
        this.anniversaryWithParentNames = anniversaryWithParentNames;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_anniversary_delete, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setText();
        setAnniversary();
        prepareButtons();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        existsView = view.findViewById(R.id.dialog_anniversary_delete_text);
        questionView = view.findViewById(R.id.dialog_anniversary_delete_question);
        warningView = view.findViewById(R.id.dialog_anniversary_delete_warning);

        anniversaryNameView = view.findViewById(R.id.item_anniversary_name);
        anniversaryEqualityView = view.findViewById(R.id.item_anniversary_equality);
        anniversaryDefinitionView = view.findViewById(R.id.item_anniversary_definition);

        cancelButton = view.findViewById(R.id.dialog_anniversary_delete_cancel);
        overrideButton = view.findViewById(R.id.dialog_anniversary_delete_override);
    }

    protected void setText() {
        existsView.setText(existsText);
        questionView.setText(questionText);
        warningView.setText(warningText);
    }

    protected void setAnniversary() {
        anniversaryNameView.setText(anniversaryWithParentNames.anniversary.getNamePlural());
        anniversaryEqualityView.setText("1 "+ anniversaryWithParentNames.anniversary.getNameSingular()+" =");
        long amount = anniversaryWithParentNames.anniversary.getAmount();
        anniversaryDefinitionView.setText(amount+" "+ (amount == 1 ? anniversaryWithParentNames.parentSingular : anniversaryWithParentNames.parentPlural));
    }

    private void prepareButtons() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null) {
                cancelListener.onCancel();
            }
            dismiss();
        });
        overrideButton.setOnClickListener(v -> {

            if (deleteListener != null)
                AnniversaryStore.delete(anniversaryWithParentNames.anniversary, getContext(), () -> deleteListener.onAccept());
            else
                AnniversaryStore.delete(anniversaryWithParentNames.anniversary, getContext(), () -> {});
            dismiss();
        });
    }
}
