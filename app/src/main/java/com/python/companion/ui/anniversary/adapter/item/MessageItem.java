package com.python.companion.ui.anniversary.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Message;
import com.python.companion.util.AnniversaryUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class MessageItem extends AbstractItem<ViewHolder<MessageItem>> {
    public static final @LayoutRes int layoutResource = R.layout.item_text;

    private Message message;
    private @Nullable String anniversarySingular, anniversaryPlural;

    public MessageItem(Message message) {
        this.message = message;
        this.anniversarySingular = AnniversaryUtil.getBaseChronoUnitSingular(message.getType());
        this.anniversaryPlural = message.getType().toString();
    }

    @NotNull
    @Override
    public ViewHolder<MessageItem> getViewHolder(@NotNull View view) {
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

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public @Nullable String getAnniversarySingular() {
        return anniversarySingular;
    }

    public void setAnniversarySingular(@NonNull String anniversarySingular) {
        this.anniversarySingular = anniversarySingular;
    }

    public @Nullable String getAnniversaryPlural() {
        return anniversaryPlural;
    }

    public void setAnniversaryPlural(@NonNull String anniversaryPlural) {
        this.anniversaryPlural = anniversaryPlural;
    }

    @Override
    public void bindView(@NotNull ViewHolder<MessageItem> holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }

    public static class NotifyViewHolder extends ViewHolder<MessageItem> {
        private TextView nameView;

        public NotifyViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NotNull MessageItem item, @NotNull List<Object> list) {
            Message notify = item.getMessage();
            long amount = notify.getAmount();
            if (amount >= 1)
                nameView.setText(notify.getAmount() + " "+(notify.getAmount() == 1 ? item.getAnniversarySingular() : item.getAnniversaryPlural())+" before");
            else
                nameView.setText("On anniversary date itself");
        }

        @Override
        public void unbindView(@NotNull MessageItem item) {

        }
    }
}
