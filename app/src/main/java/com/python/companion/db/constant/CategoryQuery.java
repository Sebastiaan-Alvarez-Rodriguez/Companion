package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.ColorInt;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.entity.Category;

import java.util.concurrent.Executors;

public class CategoryQuery {
    private DAOCategory daoCategory;

    public CategoryQuery(Context context) {
        daoCategory = Database.getDatabase(context).getDAOCategory();
    }

    public void insert(String name, @ColorInt int color, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoCategory.insert(new Category(name, color));
            listener.onResult(null);
        });
    }

    public void update(String name, @ColorInt int color, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoCategory.update(name, color);
            listener.onResult(null);
        });
    }

    public void update(String prevName, String name, @ColorInt int color, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoCategory.update(prevName, name, color);
            listener.onResult(null);
        });
    }

    public void delete(String name, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoCategory.delete(new Category(name, 0));
            listener.onResult(null);
        });
    }

    public void isUnique(String name, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoCategory.get(name) == null));
    }

    public void isUniqueInstanced(String name, ResultListener<Category> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoCategory.get(name)));
    }
}
