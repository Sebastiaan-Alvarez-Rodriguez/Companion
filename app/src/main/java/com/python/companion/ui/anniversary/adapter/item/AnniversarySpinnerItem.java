package com.python.companion.ui.anniversary.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.ui.general.adapter.ItemAdapter;

/**
 * Unused example of how you should use the base adapter provided in {@link ItemAdapter}
 */
public class AnniversarySpinnerItem extends ItemAdapter.Item<ItemAdapter.ViewHolder<AnniversarySpinnerItem>> {
    @Override
    public int getLayoutRes() {
        return R.layout.item_text;
    }

    @NonNull
    @Override
    public AnniversaryItemViewHolder getViewHolder(@NonNull View view) {
        return new AnniversaryItemViewHolder(view);
    }

    public static class AnniversaryItemViewHolder extends ItemAdapter.ViewHolder<AnniversarySpinnerItem> {
        protected TextView textView;

        public AnniversaryItemViewHolder(@NonNull View view) {
            super(view);
            textView = view.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NonNull AnniversarySpinnerItem item) {
//            textView.setText(item.getNamePlural());
        }
    }
}
