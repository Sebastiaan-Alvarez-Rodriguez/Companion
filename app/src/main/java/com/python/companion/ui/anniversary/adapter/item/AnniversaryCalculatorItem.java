package com.python.companion.ui.anniversary.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.python.companion.R;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.ui.anniversary.Type;
import com.python.companion.util.AnniversaryUtil;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class AnniversaryCalculatorItem extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_anniversary_calculator;

    protected Anniversary anniversary;

    private Type currentType;
    private LocalDate date;
    private long distance;
    private boolean hasError;
    private String error;

    protected AnniversaryCalculatorItem(Anniversary anniversary) {
        this.anniversary = anniversary;
        this.currentType = Type.DATE;
    }

    public AnniversaryCalculatorItem(Anniversary anniversary, LocalDate date) {
        this(anniversary);
        this.date = date;
        this.distance = AnniversaryUtil.computeDistance(date);
        this.hasError = false;
    }

    public AnniversaryCalculatorItem(Anniversary anniversary, String error) {
        this(anniversary);
        this.hasError = true;
        this.error = error;
    }

    @NotNull
    @Override
    public FastAdapter.ViewHolder getViewHolder(@NotNull View view) {
        return new AnniversaryViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_cactus_layout;
    }

    public Anniversary getAnniversary() {
        return anniversary;
    }

    /** Returns currently displayed value. Can either be a date, distance, or a given error */
    public String getDisplayValue() {
        return hasError ? error : (currentType == Type.DATE ? date.toString() : String.valueOf(distance));
    }

    public String getAnniversaryName() {
        return currentType == Type.DISTANCE && distance == 1 ? anniversary.getNameSingular() : anniversary.getNamePlural();
    }

    @Override
    public void bindView(@NotNull FastAdapter.ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }

    /** Called when user changes type. Does not require recomputing date/distance.
     * !!! Manually invalidate view after update!!! This object merely represents an item, not a view
     */
    public void onTypeChange(Type type) {
        if (type != currentType) {
            currentType = type;
        }
    }

    public void onDateChange(LocalDate date) {
        this.date = date;
        this.distance = AnniversaryUtil.computeDistance(date);
        this.hasError = false;
    }

    public void onDateError(@Nullable String msg) {
        hasError = true;
        error = msg == null ? "!" : msg;
    }


    /** Viewholder for {@link AnniversaryCalculatorItem#layoutResource} */
    public static class AnniversaryViewHolder extends FastAdapter.ViewHolder<AnniversaryCalculatorItem> {
        private TextView amountView, nameView;


        public AnniversaryViewHolder(@NotNull View itemView) {
            super(itemView);
            amountView = itemView.findViewById(R.id.item_cactus_amount);
            nameView = itemView.findViewById(R.id.item_cactus_name);
        }

        @Override
        public void bindView(@NotNull AnniversaryCalculatorItem item, @NotNull List<Object> list) {
            amountView.setText(item.getDisplayValue());
            nameView.setText(item.getAnniversaryName());
        }

        @Override
        public void unbindView(@NotNull AnniversaryCalculatorItem item) {
        }
    }
}
