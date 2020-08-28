package com.python.companion.ui.notes.category.activity;

import android.app.Application;
import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
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
import com.mikepenz.fastadapter.extensions.ExtensionsFactories;
import com.mikepenz.fastadapter.select.SelectExtension;
import com.mikepenz.fastadapter.select.SelectExtensionFactory;
import com.python.companion.R;
import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.repository.CategoryRepository;
import com.python.companion.ui.general.customviews.ContextMenuRecyclerView;
import com.python.companion.ui.notes.category.adapter.CategoryItem;
import com.python.companion.ui.notes.category.dialog.CategoryDeleteDialog;
import com.python.companion.ui.notes.category.dialog.CategoryUpdateDialog;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CategoryEditActivity extends AppCompatActivity {
    private View layout;
    private TextView colorView;
    private EditText newCategoryName;
    private ImageView newCategoryAdd;

    private ContextMenuRecyclerView list;

    private ItemAdapter<CategoryItem> itemAdapter;
    private FastAdapter<CategoryItem> fastAdapter;
    protected SelectExtension<CategoryItem> selectionExtension;

    private CategoryEditViewModel viewmodel;

    private String initialName;
    private @ColorInt int initialColor;

    private boolean selectedInitial;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_edit);
        viewmodel = new ViewModelProvider(this).get(CategoryEditViewModel.class);

        selectedInitial = false;

        Intent intent = getIntent();
        if (intent.hasExtra("categoryName") && intent.getStringExtra("categoryName").length() != 0) {
            initialName = intent.getStringExtra("categoryName");
            initialColor = intent.getIntExtra("categoryColor", ContextCompat.getColor(this, R.color.colorPrimary));
        } else {
            initialName = "<default>";
            initialColor = ContextCompat.getColor(this, R.color.colorPrimary);
        }

        findViews();
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
    }

    private void prepareList() {
        itemAdapter = new ItemAdapter<>();
        fastAdapter = FastAdapter.with(itemAdapter);
        ExtensionsFactories.INSTANCE.register(new SelectExtensionFactory());
        registerForContextMenu(list);

        list.setAdapter(fastAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        selectionExtension = fastAdapter.getOrCreateExtension(SelectExtension.class);
        assert selectionExtension != null;
        selectionExtension.setSelectable(true);
        selectionExtension.setMultiSelect(false);
        selectionExtension.setSelectOnLongClick(false);

        fastAdapter.setOnClickListener((view, categoryItemIAdapter, categoryItem, pos) -> {
            selectionExtension.toggleSelection(pos);
            categoryItem.setSelected(!categoryItem.isSelected());
            fastAdapter.notifyItemChanged(pos);
            return true;
        });

        fastAdapter.setOnLongClickListener((view, categoryItemIAdapter, categoryItem, integer) -> false);
        setListUpdates();
    }

    private void setListUpdates() {
        viewmodel.getCategories().observe(this, categories -> {
            List<CategoryItem> items = categories.parallelStream().filter(category -> !category.getCategoryName().equals("<default>")).map(category -> {
                CategoryItem item = new CategoryItem();
                item.setCategory(category);
                return item;
            }).collect(Collectors.toList());
            List<CategoryItem> withDefault = new ArrayList<>(1+categories.size());

            CategoryItem c = new CategoryItem();
            c.setCategory(new Category("<default>", ContextCompat.getColor(this, R.color.colorPrimary)));
            withDefault.add(c);

            withDefault.addAll(items);
            itemAdapter.set(withDefault);

            if (!selectedInitial) {
                for (int x = 0; x < fastAdapter.getItemCount(); ++x) {
                    CategoryItem item = fastAdapter.getItem(x);
                    if (item != null && item.getCategory().getCategoryName().equals(initialName)) {
                        int location = fastAdapter.getPosition(item);
                        selectionExtension.toggleSelection(location);
                        item.setSelected(true);
                        fastAdapter.notifyItemChanged(location);
                        selectedInitial = true;
                        break;
                    }
                }
            }
        });
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
                        categoryQuery.insert(newName, initialColor, x -> {});
                    else
                        Snackbar.make(layout, "Category already exists", Snackbar.LENGTH_LONG).show();
                });
            }
        });
        colorView.setOnClickListener(v -> new ColorPickerDialog.Builder(this)
                .setTitle("Pick a color")
                .setPositiveButton("Pick", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    initialColor = envelope.getColor();
                    colorView.setBackgroundColor(initialColor);
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
        ContextMenuRecyclerView.RecyclerViewContextMenuInfo info = (ContextMenuRecyclerView.RecyclerViewContextMenuInfo) menuInfo;
        CategoryItem clicked = fastAdapter.getItem(info.position);
        if (clicked.getCategory().getCategoryName().equals("<default>")) { // Must not allow deleting/editing default category
            menu.close();
            return;
        }
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
                        .build();
                categoryUpdateDialog.show(getSupportFragmentManager(), null);
                break;
            case R.id.menu_context_category_edit_delete:
                CategoryDeleteDialog categoryDeleteDialog = new CategoryDeleteDialog.Builder()
                        .setCategory(clicked.getCategory())
                        .build();
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
        Set<CategoryItem> selected = selectionExtension.getSelectedItems();
        if (selected.size() == 1) {
            Category current = selected.iterator().next().getCategory();
            data.putExtra("categoryName", current.getCategoryName());
            data.putExtra("categoryColor", current.getCategoryColor());
            setResult(RESULT_OK, data);
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Go back without selecting a category? Default category will be selected")
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Go back", (dialog12, which) -> {
                        data.putExtra("categoryName", "<default>");
                        data.putExtra("categoryColor", ContextCompat.getColor(this, R.color.colorPrimary));
                        setResult(RESULT_OK, data);
                        finish();
                    })
                    .setOnCancelListener(dialog -> finish())
                    .show();
        }
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