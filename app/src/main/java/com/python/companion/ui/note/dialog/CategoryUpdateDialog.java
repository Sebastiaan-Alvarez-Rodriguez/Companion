package com.python.companion.ui.note.dialog;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.templates.dialog.DialogAcceptListener;
import com.python.companion.ui.templates.dialog.DialogCancelListener;

@SuppressWarnings("WeakerAccess")
public class CategoryUpdateDialog {
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

        public CategoryUpdateDialog build() {
            if (category == null)
                throw new IllegalStateException("Caller must specify Category to be updated with builder.setCategory()");
            return new CategoryUpdateDialog(context, dialogCancelListener, dialogAcceptListener, category);
        }
    }

    protected android.app.Dialog dialog;

    protected TextView editTextView, colorView;
    protected EditText nameEditText;
    protected Button cancelButton, acceptButton;


    protected Context context;
    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptListener acceptListener;
    protected @NonNull Category category;


    protected CategoryUpdateDialog(@NonNull Context context, @Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptListener acceptListener, @NonNull Category category) {
        this.context = context;
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.category = category;
    }

    public void showDialog(Activity activity, FragmentManager manager) {
        dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_category_edit);
        findGlobalViews();
        setCategory();
        setupClicks(activity, manager);
        dialog.show();
    }

    @CallSuper
    protected void findGlobalViews() {
        editTextView = dialog.findViewById(R.id.dialog_category_edit_text);
        colorView = dialog.findViewById(R.id.dialog_category_edit_color);
        nameEditText = dialog.findViewById(R.id.dialog_category_edit_name);
        cancelButton = dialog.findViewById(R.id.dialog_category_edit_cancel);
        acceptButton = dialog.findViewById(R.id.dialog_category_edit_accept);
    }

    protected void setCategory() {
        editTextView.setText("Editing "+category.getCategoryName());
        colorView.setBackgroundColor(category.getCategoryColor());
        nameEditText.setText(category.getCategoryName());
    }

    private void setupClicks(Activity activity, FragmentManager manager) {
        colorView.setOnClickListener(v -> {
            ColorPickerDialog dialog = ColorPickerDialog.newBuilder().setShowAlphaSlider(false).setColor(category.getCategoryColor()).create();
            dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                @Override
                public void onColorSelected(int dialogId, int c) {
                    category.setCategoryColor(c);
                    colorView.setBackgroundColor(c);
                }

                @Override
                public void onDialogDismissed(int dialogId) {}
            });
            dialog.show(manager, null);
        });

        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dialog.dismiss();
        });

        acceptButton.setOnClickListener(v -> {
            String prevName = category.getCategoryName();
            String name = nameEditText.getText().toString();
            @ColorInt int color = category.getCategoryColor();

            if (name.length() == 0) {
                nameEditText.setError("Please fill in a category name");
            } else {
                if (prevName.equals(name)) { //Unchanged name. Can safely update color
                    Log.i("Context", "Change complete. No conflict");
                    CategoryQuery categoryQuery = new CategoryQuery(context);
                    categoryQuery.update(prevName, color, x -> {});
                    NoteQuery noteQuery = new NoteQuery(context);
                    noteQuery.updateEntireCategory(prevName, name, color, x -> {});
                    if (acceptListener != null)
                        acceptListener.onAccept();
                    dialog.dismiss();
                } else { // Name changed. Unique?
                    CategoryQuery categoryQuery = new CategoryQuery(context);
                    categoryQuery.isUniqueInstanced(name, other -> {
                        if (other == null) { // Name changed & unique
                            categoryQuery.update(prevName, name, color, x -> {});
                            NoteQuery noteQuery = new NoteQuery(context);
                            noteQuery.updateEntireCategory(prevName, name, color, x -> {});
                            if (acceptListener != null)
                                acceptListener.onAccept();
                            dialog.dismiss();
                        } else { // Name changed & conflict
                            activity.runOnUiThread(() -> {
                            CategoryMergeDialog categoryMergeDialog = new CategoryMergeDialog.Builder(v.getContext())
                                    .setFinishListener(() -> {
                                        if (acceptListener != null)
                                            acceptListener.onAccept();
                                        dialog.dismiss();
                                    })
                                    .setOldCategory(category)
                                    .setNewCategory(other)
                                    .build();
                            categoryMergeDialog.showDialog();
                            });
                        }
                    });
                }
            }
        });
    }
}