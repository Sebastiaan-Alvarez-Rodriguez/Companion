package com.python.companion.ui.note.activity.edit.category;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.python.companion.backend.category.CategoryRepository;
import com.python.companion.backend.note.NoteRepository;
import com.python.companion.db.entity.Category;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {
    private CategoryRepository categoryRepository;
    private NoteRepository noteRepository;

    private LiveData<List<Category>> categories = null;
    private LiveData<Category> currentCategory = null;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        categoryRepository = new CategoryRepository(application);
        noteRepository = new NoteRepository(application);
    }

    public LiveData<List<Category>> getCategories() {
        if (categories == null)
            categories = categoryRepository.getUniqueCategories();
        return categories;
    }

    public LiveData<Category> getCurrentCategory(String name) {
        if (currentCategory == null)
            currentCategory = noteRepository.getCategoryForNote(name);
        return currentCategory;
    }
}
