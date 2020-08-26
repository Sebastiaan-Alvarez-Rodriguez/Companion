package com.python.companion.ui.anniversary.adapter.item;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class AnniversaryItem extends AbstractItem<FastAdapter.ViewHolder<AnniversaryItem>> {
    public static final @LayoutRes
    int layoutResource = R.layout.item_anniversary;

    private Anniversary anniversary;
    private @NonNull String parentSingular;
    private @NonNull String parentPlural;

    public AnniversaryItem(@NonNull AnniversaryWithParentNames anniversaryWithParentNames) {
        this.anniversary = anniversaryWithParentNames.anniversary;

        this.parentSingular = Objects.requireNonNull(anniversaryWithParentNames.parentSingular);
        this.parentPlural = Objects.requireNonNull(anniversaryWithParentNames.parentPlural);
    }

    public AnniversaryItem(@NonNull Anniversary anniversary, @NonNull String parentSingular, @NonNull String parentPlural) {
        this.anniversary = anniversary;
        this.parentSingular = parentSingular;
        this.parentPlural = parentPlural;
    }

    @NotNull
    @Override
    public FastAdapter.ViewHolder<AnniversaryItem> getViewHolder(@NotNull View view) {
        return new AnniversaryItem.AnniversaryViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_anniversary_layout;
    }

    @Override
    public long getIdentifier() {
        return anniversary.getAnniversaryID();
    }

    public Anniversary getAnniversary() {
        return anniversary;
    }

    public void setAnniversary(Anniversary anniversary) {
        this.anniversary = anniversary;
    }

    @NonNull
    public String getParentSingular() {
        return parentSingular;
    }

    public void setParentSingular(@NonNull String parentSingular) {
        this.parentSingular = parentSingular;
    }

    @NonNull
    public String getParentPlural() {
        return parentPlural;
    }

    public void setParentPlural(@NonNull String parentPlural) {
        this.parentPlural = parentPlural;
    }

    @Override
    public void bindView(@NotNull FastAdapter.ViewHolder<AnniversaryItem> holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }


    public static class AnniversaryViewHolder extends FastAdapter.ViewHolder<AnniversaryItem> {
        private TextView nameView, equalityView, definitionView;
        private ImageView notificationView;

        public AnniversaryViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_anniversary_name);
            equalityView = itemView.findViewById(R.id.item_anniversary_equality);
            definitionView = itemView.findViewById(R.id.item_anniversary_definition);
            notificationView = itemView.findViewById(R.id.item_anniversary_notifications);
        }

        @Override
        public void bindView(@NotNull AnniversaryItem item, @NotNull List<Object> list) {
            nameView.setText(item.getAnniversary().getNamePlural());
            equalityView.setText("1 "+item.getAnniversary().getNameSingular()+" =");

            long amount = item.getAnniversary().getAmount();
            String parentname = amount == 1 ? item.getParentSingular() : item.getParentPlural();
            definitionView.setText(amount +" "+parentname);

            notificationView.setImageResource(item.getAnniversary().getHasNotifications() ? R.drawable.ic_notification : R.drawable.ic_notification_off);
        }

        @Override
        public void unbindView(@NotNull AnniversaryItem item) {

        }
    }
}
