package com.python.companion.ui.notes.category.dialog.set;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.python.companion.backend.category.CategoryRepository;
import com.python.companion.db.entity.Category;

import java.util.List;

public class CategorySetDialogViewModel extends AndroidViewModel {
    private CategoryRepository categoryRepository;

    private LiveData<List<Category>> categories = null;

    public CategorySetDialogViewModel(@NonNull Application application) {
        super(application);
        categoryRepository = new CategoryRepository(application);
    }

    public LiveData<List<Category>> getCategories() {
        if (categories == null)
            categories = categoryRepository.getUniqueCategories();
        return categories;
    }
}
