package com.python.companion.ui.jubileum.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.ui.general.adapter.ItemAdapter;

/**
 * Unused example of how you should use the base adapter provided in {@link ItemAdapter}
 */
public class JubileumSpinnerItem extends ItemAdapter.Item<ItemAdapter.ViewHolder<JubileumSpinnerItem>> {
    @Override
    public int getLayoutRes() {
        return R.layout.item_text;
    }

    @NonNull
    @Override
    public JubileumItemViewHolder getViewHolder(@NonNull View view) {
        return new JubileumItemViewHolder(view);
    }

    public static class JubileumItemViewHolder extends ItemAdapter.ViewHolder<JubileumSpinnerItem> {
        protected TextView textView;

        public JubileumItemViewHolder(@NonNull View view) {
            super(view);
            textView = view.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NonNull JubileumSpinnerItem item) {
//            textView.setText(item.getNamePlural());
        }
    }
}
