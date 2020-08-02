package com.python.companion.ui.jubileum.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Notify;

import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.List;

@SuppressWarnings("unused")
public class NotifyItem extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_text;

    private Notify notify;

    public NotifyItem(Notify notify) {
        this.notify = notify;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new NotifyViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_text_layout;
    }

    public Notify getNotify() {
        return notify;
    }

    public void setNotify(Notify notify) {
        this.notify = notify;
    }

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
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
            ChronoUnit cornerstoneType = notify.getCornerstoneType();
            if (amount > 1) {
                nameView.setText(notify.getAmount() + " " + cornerstoneType.name() + " before");
            } else if (amount == 1) {
                nameView.setText(notify.getAmount() + " " + cornerstoneType.name().substring(0, cornerstoneType.name().length()-1) + " before");
            } else {
                nameView.setText("On jubileum date itself");
            }
        }

        @Override
        public void unbindView(@NotNull NotifyItem item) {}
    }
}
