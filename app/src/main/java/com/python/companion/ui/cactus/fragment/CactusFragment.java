package com.python.companion.ui.cactus.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.python.companion.R;
import com.python.companion.ui.cactus.activity.measurement.MeasurementAddActivity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class CactusFragment extends Fragment {
    private TextView quoteView, yearView, monthView, dayView;
    private ImageView cactusView;

    private LocalDate together, now;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        now = LocalDate.now();
        SharedPreferences preferences = getContext().getSharedPreferences(getString(R.string.measurement_preferences), Context.MODE_PRIVATE);
        together = LocalDate.parse(preferences.getString("together", "2017-11-08"));
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cactus, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);
        prepareAdd();
        updateStats(true);
    }

    private void findViews(View view) {
        quoteView = view.findViewById(R.id.fragment_cactus_quote);
        yearView = view.findViewById(R.id.fragment_cactus_years);
        monthView = view.findViewById(R.id.fragment_cactus_months);
        dayView = view.findViewById(R.id.fragment_cactus_days);
        cactusView = view.findViewById(R.id.fragment_cactus_cactus);
    }

    private void prepareAdd() {
        cactusView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MeasurementAddActivity.class);
            startActivity(intent);
        });
    }

    private void updateStats() {
        updateStats(false);
    }

    private void updateStats(boolean force) {
        LocalDate now = LocalDate.now();
        if (!force && now.equals(this.now))
            return;
        this.now = now;
        yearView.setText(String.valueOf(ChronoUnit.YEARS.between(together, this.now)));
        monthView.setText(String.valueOf(ChronoUnit.MONTHS.between(together, this.now)%12));
        dayView.setText(String.valueOf(ChronoUnit.DAYS.between(together, this.now)));//TODO: Should fix to cumulative distance
    }
}