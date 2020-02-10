package com.python.companion.ui.note.dialog;

import android.content.Context;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.templates.dialog.DialogAcceptListener;
import com.python.companion.ui.templates.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class CategoryDeleteDialog {

    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptListener dialogAcceptListener = null;

        private Context context;

        private Category category = null;


        public Builder(@NonNull Context context) {
            this.context = context;
        }

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
            return new CategoryDeleteDialog(context, dialogCancelListener, dialogAcceptListener, category);
        }
    }

    protected android.app.Dialog dialog;

    protected Button cancelButton, deleteButton;
    protected TextView categoryNameView, categoryColorView;

    protected Context context;
    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener acceptListener;
    final protected Category category;

    protected CategoryDeleteDialog(@NonNull Context context, @Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener acceptListener, @NonNull Category category) {
        this.context = context;
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.category = category;
    }

    public void showDialog() {
        dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_category_delete);
        findGlobalViews();
        setCategory();
        setupClicks();
        dialog.show();
    }

    @CallSuper
    protected void findGlobalViews() {
        categoryNameView = dialog.findViewById(R.id.item_category_name);
        categoryColorView =  dialog.findViewById(R.id.item_category_color);
        cancelButton = dialog.findViewById(R.id.dialog_category_delete_cancel);
        deleteButton = dialog.findViewById(R.id.dialog_category_delete_accept);
    }

    protected void setCategory() {
        categoryColorView.setBackgroundColor(category.getCategoryColor());
        categoryNameView.setText(category.getCategoryName());
    }

    private void setupClicks() {
        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dialog.dismiss();
        });
        deleteButton.setOnClickListener(v -> {
            NoteQuery noteQuery = new NoteQuery(context);
            String defaultName = "";
            @ColorInt int defaultColor = ContextCompat.getColor(context, R.color.colorPrimary);
            noteQuery.updateEntireCategory(category.getCategoryName(), defaultName, defaultColor, x -> {});

            CategoryQuery categoryQuery = new CategoryQuery(context);
            categoryQuery.delete(category.getCategoryName(), x -> {});

            if (acceptListener != null)
                acceptListener.onAccept();
            dialog.dismiss();
        });
    }

}
