package com.python.companion.ui.note.dialog.merge;

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
import androidx.fragment.app.DialogFragment;

import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.templates.dialog.DialogAcceptListener;
import com.python.companion.ui.templates.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class CategoryMergeDialog extends DialogFragment {
    
    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private Category oldCategory = null, newCategory = null;


        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setFinishListener(DialogAcceptListener dialogAcceptListener) {
            this.dialogAcceptListener = dialogAcceptListener;
            return this;
        }

        public Builder setOldCategory(Category category) {
            this.oldCategory = category;
            return this;
        }

        public Builder setNewCategory(Category category) {
            this.newCategory = category;
            return this;
        }

        public CategoryMergeDialog build() {
            if (oldCategory == null || newCategory == null)
                throw new IllegalStateException("Both oldCategory and newCategory must be provided by caller with builder.setOldCategory(category) and builder.setNewCategory(category)");
            return new CategoryMergeDialog(dialogCancelListener, dialogAcceptListener, oldCategory, newCategory);
        }
    }

    protected TextView oldColorView, newColorView, oldNameView, newNameView;
    protected Button cancelButton, mergeButton;


    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener acceptListener;
    final protected Category oldCategory, newCategory;

    protected CategoryMergeDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener acceptListener, @NonNull Category oldCategory, @NonNull Category newCategory) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.oldCategory = oldCategory;
        this.newCategory = newCategory;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_category_merge, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findGlobalViews(view);
        setCategories();
        setupClicks();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        View oldView = view.findViewById(R.id.dialog_category_merge_from);
        oldColorView = oldView.findViewById(R.id.item_category_color);
        oldNameView = oldView.findViewById(R.id.item_category_name);

        View newView = view.findViewById(R.id.dialog_category_merge_to);
        newColorView = newView.findViewById(R.id.item_category_color);
        newNameView = newView.findViewById(R.id.item_category_name);
        cancelButton = view.findViewById(R.id.dialog_category_merge_cancel);
        mergeButton = view.findViewById(R.id.dialog_category_merge_accept);
    }

    protected void setCategories() {
        oldColorView.setBackgroundColor(oldCategory.getCategoryColor());
        oldNameView.setText(oldCategory.getCategoryName());
        newColorView.setBackgroundColor(newCategory.getCategoryColor());
        newNameView.setText(newCategory.getCategoryName());
    }

    private void setupClicks() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });
        mergeButton.setOnClickListener(v -> {
            String prevName = oldCategory.getCategoryName(), name = newCategory.getCategoryName();
            @ColorInt int color = newCategory.getCategoryColor();

            NoteQuery noteQuery = new NoteQuery(getContext());
            noteQuery.updateEntireCategory(prevName, name, color, x -> {});

            CategoryQuery categoryQuery = new CategoryQuery(getContext());
            categoryQuery.delete(prevName, x -> {});

            if (acceptListener != null)
                acceptListener.onAccept();
            dismiss();
        });
    }

}
