package com.python.companion.ui.anniversary.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Anniversary;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class AnniversaryItemSimple extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_text;

    private Anniversary anniversary;

    private boolean displayPlural;

    public AnniversaryItemSimple(@NonNull Anniversary anniversary) {
        this(anniversary, true);
    }

    public AnniversaryItemSimple(@NonNull Anniversary anniversary, boolean displayPlural) {
        this.anniversary = anniversary;
        this.displayPlural = displayPlural;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new AnniversarySimpleViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_anniversary_layout;
    }

    public Anniversary getAnniversary() {
        return anniversary;
    }

    public void setAnniversary(Anniversary anniversary) {
        this.anniversary = anniversary;
    }

    public boolean getDisplayPlural() {
        return displayPlural;
    }

    public void setDisplayPlural(boolean displayPlural) {
        this.displayPlural = displayPlural;
    }

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }

    @Override
    public long getIdentifier() {
        return anniversary.getAnniversaryID();
    }

    public static class AnniversarySimpleViewHolder extends ViewHolder<AnniversaryItemSimple> {
        private TextView nameView;


        public AnniversarySimpleViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NotNull AnniversaryItemSimple item, @NotNull List<Object> list) {
            nameView.setText(item.getDisplayPlural() ? item.getAnniversary().getNamePlural() : item.getAnniversary().getNameSingular());
        }

        @Override
        public void unbindView(@NotNull AnniversaryItemSimple item) {}
    }
}
