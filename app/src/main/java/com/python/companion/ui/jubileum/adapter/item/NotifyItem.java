package com.python.companion.ui.jubileum.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Notify;
import com.python.companion.db.pojo.notify.NotifyWithMeasurementNames;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class NotifyItem extends AbstractItem<ViewHolder<NotifyItem>> {
    public static final @LayoutRes int layoutResource = R.layout.item_text;

    private Notify notify;
    private @Nullable String measurementSingular, measurementPlural;

    public NotifyItem(NotifyWithMeasurementNames notifyWithMeasurementNames) {
        this.notify = notifyWithMeasurementNames.notify;
        this.measurementSingular = notifyWithMeasurementNames.measurementSingular;
        this.measurementPlural = notifyWithMeasurementNames.measurementPlural;
    }

    @NotNull
    @Override
    public ViewHolder<NotifyItem> getViewHolder(@NotNull View view) {
        return new NotifyViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_jubileum_layout;
    }

    public Notify getNotify() {
        return notify;
    }

    public void setNotify(Notify notify) {
        this.notify = notify;
    }

    public @Nullable String getMeasurementSingular() {
        return measurementSingular;
    }

    public void setMeasurementSingular(@NonNull String measurementSingular) {
        this.measurementSingular = measurementSingular;
    }

    public @Nullable String getMeasurementPlural() {
        return measurementPlural;
    }

    public void setMeasurementPlural(@NonNull String measurementPlural) {
        this.measurementPlural = measurementPlural;
    }

    @Override
    public void bindView(@NotNull ViewHolder<NotifyItem> holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }

    public static class NotifyViewHolder extends ViewHolder<NotifyItem> {
        private TextView nameView;

        public NotifyViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NotNull NotifyItem item, @NotNull List<Object> list) {
            Notify notify = item.getNotify();
            long amount = notify.getAmount();
            if (amount >= 1)
                nameView.setText(notify.getAmount() + " "+(notify.getAmount() == 1 ? item.getMeasurementSingular() : item.getMeasurementPlural())+" before");
            else
                nameView.setText("On jubileum date itself");
        }

        @Override
        public void unbindView(@NotNull NotifyItem item) {

        }
    }
}
