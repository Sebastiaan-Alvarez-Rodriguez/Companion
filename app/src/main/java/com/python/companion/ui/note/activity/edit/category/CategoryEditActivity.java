package com.python.companion.ui.note.activity.edit.category;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.ui.category.list.adapter.CategoryAdapterCheckable;

public class CategoryEditActivity extends AppCompatActivity {

    private View layout;
    private TextView colorView;
    private EditText newCategoryName;
    private ImageView newCategoryAdd;
    private Button doneButton;

    private RecyclerView list;
    private CategoryAdapterCheckable adapter;
    private CategoryViewModel categoryViewModel;

    private String noteName;
    private @ColorInt int color;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_edit);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        noteName = getIntent().getStringExtra("name");
        color = ContextCompat.getColor(this, R.color.colorPrimary);
        findViews();
        prepareList();
        setupClicks();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_category_edit_layout);
        colorView = findViewById(R.id.activity_category_edit_color);
        newCategoryName = findViewById(R.id.activity_category_edit_new);
        newCategoryAdd = findViewById(R.id.activity_category_edit_add_new);
        doneButton = findViewById(R.id.activity_category_edit_done);
        list = findViewById(R.id.activity_category_edit_list);
    }

    private void setupClicks() {
        newCategoryAdd.setOnClickListener(v -> {
            String newName = newCategoryName.getText().toString();
            if (newName.length() == 0) {
                newCategoryName.setError("Please fill in this field");
            } else {
                CategoryQuery categoryQuery = new CategoryQuery(this);
                categoryQuery.isUnique(newName, unique -> {
                    if (unique) {
                        categoryQuery.insert(newName, 0, x -> {}); //TODO: Fetch true color
                    } else {
                        Snackbar.make(layout, "Category already exists", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
        doneButton.setOnClickListener(v -> finish());
        colorView.setOnClickListener(v -> {
                ColorPickerDialog dialog = ColorPickerDialog.newBuilder().setShowAlphaSlider(false).setColor(color).create();
                dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                    @Override
                    public void onColorSelected(int dialogId, int c) {
                        color = c;
                        colorView.setBackgroundColor(c);
                    }

                    @Override
                    public void onDialogDismissed(int dialogId) {}
                });
                dialog.show(getSupportFragmentManager(), null);
        });
    }

    private void prepareList() {
        adapter = new CategoryAdapterCheckable();
        categoryViewModel.getCategories().observe(this, adapter);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
    }
}