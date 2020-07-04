package com.python.companion.ui.general.spans;

import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.StyleSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class ColoredStyleSpan extends StyleSpan {
    protected @ColorInt int color;

    public ColoredStyleSpan(int style, @ColorInt int color) {
        super(style);
        this.color = color;
    }

    public ColoredStyleSpan(@NonNull Parcel src) {
        super(src);
        this.color = src.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.color);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setColor(this.color);
    }
}
