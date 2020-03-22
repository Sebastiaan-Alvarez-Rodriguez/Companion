package com.python.companion.ui.general.picker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.DatePicker;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class GoodDatePicker extends DatePicker {
    public GoodDatePicker(Context context) {
        super(context);
    }
    public GoodDatePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public GoodDatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public GoodDatePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener onDateChangedListener) {
        super.init(year, monthOfYear, dayOfMonth-1, (view, year1, monthOfYear1, dayOfMonth1) -> onDateChangedListener.onDateChanged(view, year1, monthOfYear1-1, dayOfMonth1));
    }

    /** Selects given date in picker widget. Expects human ISO-8601-like dates:
     *  2017, 11, 08 now represents 2017-11-08 (in normal Datepicker would represent 2017-12-08)
     */
    @Override
    public void updateDate(int year, int month, int dayOfMonth) {
        super.updateDate(year, month-1, dayOfMonth);
    }

    /** Selects given date in picker widget **/
    public void updateDate(@NonNull LocalDate date) {
        updateDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    public void updateDate(@NonNull String date) throws DateTimeParseException {
        updateDate(LocalDate.parse(date));
    }

    public void updateDate(@NonNull String date, @NonNull DateTimeFormatter formatter) throws DateTimeParseException {
        updateDate(LocalDate.parse(date, formatter));
    }

    @Override
    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        super.setOnDateChangedListener((view, year, monthOfYear, dayOfMonth) -> onDateChangedListener.onDateChanged(view, year, monthOfYear+1, dayOfMonth));
    }

    @Override
    public int getYear() {
        return super.getYear();
    }

    @Override
    public int getMonth() {
        return super.getMonth()+1;
    }

    @Override
    public int getDayOfMonth() {
        return super.getDayOfMonth();
    }

    public @NonNull LocalDate getDate() {
        return LocalDate.of(getYear(), getMonth(), getDayOfMonth());
    }
}
