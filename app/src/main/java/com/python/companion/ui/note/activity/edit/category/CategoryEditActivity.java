package com.python.companion.ui.note.activity.edit.category;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.ui.note.adapter.CategoryItem;
import com.python.companion.ui.note.dialog.delete.CategoryDeleteDialog;
import com.python.companion.ui.note.dialog.update.CategoryUpdateDialog;
import com.python.companion.util.ContextMenuRecyclerView;

import java.util.stream.Collectors;

public class CategoryEditActivity extends AppCompatActivity {

    private View layout;
    private TextView colorView;
    private EditText newCategoryName;
    private ImageView newCategoryAdd;

    private TextView curColorView, curNameView;

    private ContextMenuRecyclerView list;
    private FastAdapter<CategoryItem> fastAdapter;

    private CategoryViewModel categoryViewModel;

    private String noteName;

    private @ColorInt int color;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_edit);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        noteName = getIntent().getStringExtra("noteName");

        color = ContextCompat.getColor(this, R.color.colorPrimary);
        findViews();
        prepareCurrentCategoryView();
        prepareList();

        setupClicks();
        setupActionBar();
    }

    private void findViews() {
        layout = findViewById(R.id.activity_category_edit_layout);
        colorView = findViewById(R.id.activity_category_edit_color);
        newCategoryName = findViewById(R.id.activity_category_edit_new);
        newCategoryAdd = findViewById(R.id.activity_category_edit_add_new);
        list = findViewById(R.id.activity_category_edit_list);

        curColorView = findViewById(R.id.item_category_color);
        curNameView = findViewById(R.id.item_category_name);
    }

    private void prepareCurrentCategoryView() {
        categoryViewModel.getCurrentCategory(noteName).observe(this, category -> {
            if (category != null && category.getCategoryName().length() != 0) {
                Log.i("Observer", "Category for note "+noteName+" found category: " + category.getCategoryName());
                curNameView.setText(category.getCategoryName());
                curColorView.setBackgroundColor(category.getCategoryColor());
            } else {
                curNameView.setText("<Default>");
                curColorView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                Log.i("Observer", "Category for note "+noteName+" not found at this time");
            }
        });
    }

    private void prepareList() {
        ItemAdapter<CategoryItem> itemAdapter = new ItemAdapter<>();
        fastAdapter = FastAdapter.with(itemAdapter);

        registerForContextMenu(list);

        fastAdapter.setOnClickListener((view, categoryItemIAdapter, categoryItem, pos) -> {
            Log.i("Clicked", "You clicked on cat "+categoryItem.getCategory().getCategoryName());
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.updateCategory(noteName, categoryItem.getCategory(), v -> {});
            return true;
        });

        fastAdapter.setOnLongClickListener((view, categoryItemIAdapter, categoryItem, integer) -> false);

        categoryViewModel.getCategories().observe(this, categories -> itemAdapter.set(categories.stream().map(category -> {
            CategoryItem item = new CategoryItem();
            item.setCategory(category);
            return item;
        }).collect(Collectors.toList())));

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(fastAdapter);
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
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
                        categoryQuery.insert(newName, color, x -> {});
                    } else {
                        Snackbar.make(layout, "Category already exists", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
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

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_category_edit_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle("Categories");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_category_edit_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuRecyclerView.RecyclerViewContextMenuInfo info = (ContextMenuRecyclerView.RecyclerViewContextMenuInfo) item.getMenuInfo();
        CategoryItem clicked = fastAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case R.id.menu_note_context_category_edit:
                CategoryUpdateDialog categoryUpdateDialog = new CategoryUpdateDialog.Builder()
                        .setCategory(clicked.getCategory()).build();
                categoryUpdateDialog.show(getSupportFragmentManager(), null);
                break;
            case R.id.menu_note_context_category_delete:
                CategoryDeleteDialog categoryDeleteDialog = new CategoryDeleteDialog.Builder()
                        .setCategory(clicked.getCategory()).build();
                categoryDeleteDialog.show(getSupportFragmentManager(), null);
                break;
        }
        return super.onContextItemSelected(item);
    }
}