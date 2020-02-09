package com.python.companion.backend.category;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.entity.Category;

import java.util.List;

public class CategoryRepository {
    private DAOCategory daoCategory;

    public CategoryRepository(Context context) {
        daoCategory = Database.getDatabase(context).getDAOCategory();
    }

    public LiveData<List<Category>> getUniqueCategories() {
        return daoCategory.getAllLive();
    }
}
