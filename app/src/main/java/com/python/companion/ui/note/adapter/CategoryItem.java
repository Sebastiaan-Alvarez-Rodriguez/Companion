package com.python.companion.ui.note.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Category;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class CategoryItem extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_category;

    private Category category;

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new CategoryViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_category_layout;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
    }

    @SuppressWarnings("WeakerAccess")
    public class CategoryViewHolder extends ViewHolder<CategoryItem> {
        private TextView color, name;

        public CategoryViewHolder(@NotNull View itemView) {
            super(itemView);
            color = itemView.findViewById(R.id.item_category_color);
            name = itemView.findViewById(R.id.item_category_name);
        }

        @Override
        public void bindView(@NotNull CategoryItem item, @NotNull List<Object> list) {
            color.setBackgroundColor(item.getCategory().getCategoryColor());
            name.setText(item.getCategory().getCategoryName());
        }

        @Override
        public void unbindView(@NotNull CategoryItem item) {
        }
    }
}
