package com.python.companion.ui.notes.category.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.python.companion.ui.general.dialog.DialogAcceptValueListener;
import com.python.companion.ui.general.dialog.DialogCancelListener;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

@SuppressWarnings("WeakerAccess")
public class CategoryUpdateDialog extends DialogFragment {
    @SuppressWarnings("unused")
    public static class Builder {
        private DialogCancelListener dialogCancelListener = null;
        private DialogAcceptValueListener<Category> dialogAcceptListener = null;

        private Category category = null;


        public Builder setCancelListener(DialogCancelListener dialogCancelListener) {
            this.dialogCancelListener = dialogCancelListener;
            return this;
        }

        public Builder setFinishListener(DialogAcceptValueListener<Category> dialogAcceptListener) {
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
            return new CategoryUpdateDialog(dialogCancelListener, dialogAcceptListener, category);
        }
    }

    protected TextView editTextView, colorView;
    protected EditText nameEditText;
    protected Button cancelButton, acceptButton;


    protected @Nullable DialogCancelListener cancelListener;
    protected @Nullable DialogAcceptValueListener<Category> acceptListener;
    protected @NonNull Category category;


    protected CategoryUpdateDialog(@Nullable DialogCancelListener cancelListener, @Nullable DialogAcceptValueListener<Category> acceptListener, @NonNull Category category) {
        this.cancelListener = cancelListener;
        this.acceptListener = acceptListener;
        this.category = category;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_category_edit, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        findGlobalViews(view);
        setCategory();
        setupClicks();
    }

    @CallSuper
    protected void findGlobalViews(View view) {
        editTextView = view.findViewById(R.id.dialog_category_edit_text);
        colorView = view.findViewById(R.id.dialog_category_edit_color);
        nameEditText = view.findViewById(R.id.dialog_category_edit_name);
        cancelButton = view.findViewById(R.id.dialog_category_edit_cancel);
        acceptButton = view.findViewById(R.id.dialog_category_edit_accept);
    }

    protected void setCategory() {
        editTextView.setText("Editing "+category.getCategoryName());
        colorView.setBackgroundColor(category.getCategoryColor());
        nameEditText.setText(category.getCategoryName());
    }

    private void setupClicks() {
        colorView.setOnClickListener(v -> new ColorPickerDialog.Builder(getContext())
                .setTitle("Pick a color")
                .setPositiveButton("Pick", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    category.setCategoryColor(envelope.getColor());
                    colorView.setBackgroundColor(envelope.getColor());
                })
                .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .show());

        cancelButton.setOnClickListener(v -> {
            if (cancelListener != null)
                cancelListener.onCancel();
            dismiss();
        });

        acceptButton.setOnClickListener(v -> {
            String prevName = category.getCategoryName();
            String name = nameEditText.getText().toString();
            @ColorInt int color = category.getCategoryColor();

            if (name.length() == 0) {
                nameEditText.setError("Please fill in a category name");
            } else {
                if (prevName.equals(name)) { //Unchanged name. Can safely update color
                    CategoryQuery categoryQuery = new CategoryQuery(getContext());
                    categoryQuery.update(prevName, color, x -> {});
                    NoteQuery noteQuery = new NoteQuery(getContext());
                    noteQuery.updateEntireCategory(prevName, name, color, x -> {});
                    if (acceptListener != null)
                        acceptListener.onAccept(new Category(name, color));
                    dismiss();
                } else { // Name changed. Unique?
                    CategoryQuery categoryQuery = new CategoryQuery(getContext());
                    categoryQuery.isUniqueInstanced(name, other -> {
                        if (other == null) { // Name changed & unique
                            categoryQuery.update(prevName, name, color, x -> {});
                            NoteQuery noteQuery = new NoteQuery(getContext());
                            noteQuery.updateEntireCategory(prevName, name, color, x -> {});
                            if (acceptListener != null)
                                acceptListener.onAccept(new Category(name, color));
                            dismiss();
                        } else { // Name changed & conflict
                            CategoryMergeDialog categoryMergeDialog = new CategoryMergeDialog.Builder()
                                    .setFinishListener(() -> {
                                        if (acceptListener != null)
                                            acceptListener.onAccept(new Category(name, color));
                                        dismiss();
                                    })
                                    .setOldCategory(category)
                                    .setNewCategory(other)
                                    .build();
                            categoryMergeDialog.show(getChildFragmentManager(), null);
                        }
                    });
                }
            }
        });
    }
}