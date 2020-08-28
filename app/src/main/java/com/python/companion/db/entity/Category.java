package com.python.companion.db.entity;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.python.companion.util.migration.EntityVisitor;

@Entity(primaryKeys = {"categoryName"})
public class Category implements EntityVisitor.Visitable {
    private @NonNull String categoryName;
    private @ColorInt int categoryColor;

    public Category(@NonNull String categoryName, int categoryColor) {
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
    }

    @Ignore
    public Category(@NonNull Category other) {
        this(other.categoryName, other.categoryColor);
    }

    public @NonNull String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(@NonNull String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(int categoryColor) {
        this.categoryColor = categoryColor;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Category))
            return false;
        Category other = (Category) obj;
        return this.categoryName.equals(other.getCategoryName()) && this.categoryColor == other.getCategoryColor();
    }

    @Override
    public void accept(@NonNull EntityVisitor visitor) {
        visitor.visit(this);
    }
}
