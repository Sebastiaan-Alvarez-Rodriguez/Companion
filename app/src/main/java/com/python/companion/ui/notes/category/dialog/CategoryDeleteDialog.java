package com.python.companion.ui.notes.category.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.general.dialog.DialogAcceptListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class CategoryDeleteDialog extends DialogFragment {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private Category category = null;

        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setFinishListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setCategory(Category category) {
            this.category = category;
            return this;
        }

        public CategoryDeleteDialog build() {
            if (category == null)
                throw new IllegalStateException("Category must be provided by caller with builder.setCategory(category)");
            return new CategoryDeleteDialog(dialogCancelListener, dialogAcceptListener, category);
        }
    }

    protected Button cancelButton, deleteButton;
    protected TextView categoryNameView, categoryColorView;

    protected Context context;
    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener acceptListener;
    final protected Category category;

    protected CategoryDeleteDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener acceptListener, @NonNull Category category) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.category = category;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_category_delete, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setCategory();
        setupClicks();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        categoryNameView = view.findViewById(R.id.item_category_name);
        categoryColorView =  view.findViewById(R.id.item_category_color);
        cancelButton = view.findViewById(R.id.dialog_category_delete_cancel);
        deleteButton = view.findViewById(R.id.dialog_category_delete_accept);
    }

    protected void setCategory() {
        categoryColorView.setBackgroundColor(category.getCategoryColor());
        categoryNameView.setText(category.getCategoryName());
    }

    private void setupClicks() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });
        deleteButton.setOnClickListener(v -> {
            NoteQuery noteQuery = new NoteQuery(getContext());
            String defaultName = "";
            @ColorInt int defaultColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
            noteQuery.updateEntireCategory(category.getCategoryName(), defaultName, defaultColor, x -> {});

            CategoryQuery categoryQuery = new CategoryQuery(getContext());
            categoryQuery.delete(category.getCategoryName(), x -> {});

            if (acceptListener != null)
                acceptListener.onAccept();
            dismiss();
        });
    }
}
