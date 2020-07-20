package com.python.companion.ui.notes.category.activity;

import android.app.Application;
import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.python.companion.R;
import com.python.companion.backend.category.CategoryRepository;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.general.customviews.ContextMenuRecyclerView;
import com.python.companion.ui.notes.category.adapter.CategoryItem;
import com.python.companion.ui.notes.category.dialog.CategoryDeleteDialog;
import com.python.companion.ui.notes.category.dialog.CategoryUpdateDialog;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryEditActivity extends AppCompatActivity {
    private View layout;
    private TextView colorView;
    private EditText newCategoryName;
    private ImageView newCategoryAdd;

    private TextView curColorView, curNameView;

    private ContextMenuRecyclerView list;
    private FastAdapter<CategoryItem> fastAdapter;

    private CategoryEditViewModel viewmodel;

    private String categoryName;
    private @ColorInt int categoryColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_edit);
        viewmodel = new ViewModelProvider(this).get(CategoryEditViewModel.class);

        Intent intent = getIntent();
        if (intent.hasExtra("categoryName") && intent.getStringExtra("categoryName").length() != 0) {
            categoryName = intent.getStringExtra("categoryName");
            categoryColor = intent.getIntExtra("categoryColor", ContextCompat.getColor(this, R.color.colorPrimary));
                    } else {
            categoryName = "<default>";
            categoryColor = ContextCompat.getColor(this, R.color.colorPrimary);
        }

        findViews();
        updateCurrentCategoryView();
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

    private void updateCurrentCategoryView() {
        curNameView.setText(categoryName);
        curColorView.setBackgroundColor(categoryColor);
            }

    private void prepareList() {
        ItemAdapter<CategoryItem> itemAdapter = new ItemAdapter<>();
        fastAdapter = FastAdapter.with(itemAdapter);

        registerForContextMenu(list);

        fastAdapter.setOnClickListener((view, categoryItemIAdapter, categoryItem, pos) -> {
            Category category = categoryItem.getCategory();
            categoryName = category.getCategoryName();
                        categoryColor = category.getCategoryColor();
            updateCurrentCategoryView();
            return true;
        });

        fastAdapter.setOnLongClickListener((view, categoryItemIAdapter, categoryItem, integer) -> false);

        viewmodel.getCategories().observe(this, categories -> itemAdapter.set(categories.stream().map(category -> {
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
                    if (unique)
                        categoryQuery.insert(newName, categoryColor, x -> {});
                    else
                        Snackbar.make(layout, "Category already exists", Snackbar.LENGTH_LONG).show();
                });
            }
        });
        colorView.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
                .setTitle("Pick a color")
                .setPositiveButton("Pick", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    categoryColor = envelope.getColor();
                    colorView.setBackgroundColor(categoryColor);
                })
                .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .show());
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_category_edit_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(new BlendModeColorFilter(getResources().getColor(R.color.colorWindowBackground, null), BlendMode.SRC_IN));
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
            finishWithIntent();
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
            case R.id.menu_context_category_edit_edit:
                CategoryUpdateDialog categoryUpdateDialog = new CategoryUpdateDialog.Builder()
                        .setCategory(clicked.getCategory())
                        .setFinishListener(category -> {
                            if (clicked.getCategory().getCategoryName().equals(categoryName)) {
                                categoryName = category.getCategoryName();
                                categoryColor = category.getCategoryColor();
                                updateCurrentCategoryView();
                            }
                        }).build();
                categoryUpdateDialog.show(getSupportFragmentManager(), null);
                break;
            case R.id.menu_context_category_edit_delete:
                CategoryDeleteDialog categoryDeleteDialog = new CategoryDeleteDialog.Builder()
                        .setCategory(clicked.getCategory())
                        .setFinishListener(() -> {
                            if (clicked.getCategory().getCategoryName().equals(categoryName)) {
                                categoryName = "<default>";
                                categoryColor = ContextCompat.getColor(this, R.color.colorPrimary);
                                updateCurrentCategoryView();
                            }
                        }).build();
                categoryDeleteDialog.show(getSupportFragmentManager(), null);
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finishWithIntent();
        super.onBackPressed();
    }

    private void finishWithIntent() {
        Intent data = new Intent();
        data.putExtra("categoryName", categoryName);
        data.putExtra("categoryColor", categoryColor);
        setResult(RESULT_OK, data);
        finish();
    }

    public static class CategoryEditViewModel extends AndroidViewModel {
        private CategoryRepository categoryRepository;

        private LiveData<List<Category>> categories = null;

        public CategoryEditViewModel(@NonNull Application application) {
            super(application);
            categoryRepository = new CategoryRepository(application);
        }

        public LiveData<List<Category>> getCategories() {
            if (categories == null)
                categories = categoryRepository.getUniqueCategories();
            return categories;
        }
    }
}