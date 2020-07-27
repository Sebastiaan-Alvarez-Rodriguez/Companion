package com.python.companion.ui.cactus;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.python.companion.R;
import com.python.companion.ui.cactus.activity.CactusCalculatorActivity;
import com.python.companion.ui.cactus.activity.CactusJubileumActivity;
import com.python.companion.ui.jubileum.activity.JubileumSelectActivity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static android.app.Activity.RESULT_OK;

//TODO: Create swipe-up-to-refresh behaviour
public class CactusFragment extends Fragment {
    private static final int REQ_SHARED = 0;


    private FloatingActionButton jubileumButton, calculatorButton;

    private LocalDate together, now;
    private TextView quoteView, yearView, monthView, dayView, yearTextView, monthTextView, dayTextView;
    private ImageView cactusView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        now = LocalDate.now();
        SharedPreferences preferences = getContext().getSharedPreferences(getString(R.string.cactus_preferences), Context.MODE_PRIVATE);
        together = LocalDate.parse(preferences.getString(getString(R.string.cactus_preferences_key_together), "2017-11-08"));
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cactus, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);
        prepareButtons();
        updateStats(true);
        setQuote();
    }

    private void findViews(View view) {
        jubileumButton = view.findViewById(R.id.fragment_cactus_jubileum);
        calculatorButton = view.findViewById(R.id.fragment_cactus_calculator);
        quoteView = view.findViewById(R.id.fragment_cactus_quote);
        yearView = view.findViewById(R.id.fragment_cactus_years);
        monthView = view.findViewById(R.id.fragment_cactus_months);
        dayView = view.findViewById(R.id.fragment_cactus_days);
        yearTextView = view.findViewById(R.id.fragment_cactus_years_text);
        monthTextView = view.findViewById(R.id.fragment_cactus_months_text);
        dayTextView = view.findViewById(R.id.fragment_cactus_days_text);
        cactusView = view.findViewById(R.id.fragment_cactus_cactus);
    }

    private void prepareButtons() {
        cactusView.setOnClickListener(v -> {
            if (jubileumButton.getVisibility() == View.INVISIBLE) {
                jubileumButton.setVisibility(View.VISIBLE);
                calculatorButton.setVisibility(View.VISIBLE);
            } else {
                jubileumButton.setVisibility(View.INVISIBLE);
                calculatorButton.setVisibility(View.INVISIBLE);
            }
        });
        jubileumButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CactusJubileumActivity.class);
            startActivity(intent);
        });
        calculatorButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), JubileumSelectActivity.class);
            startActivityForResult(intent, REQ_SHARED);
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
        long years = ChronoUnit.YEARS.between(together, this.now),
                months = ChronoUnit.MONTHS.between(together.plus(years, ChronoUnit.YEARS), this.now),
                days = ChronoUnit.DAYS.between(together.plus(months+12*years, ChronoUnit.MONTHS), this.now);
        yearView.setText(String.valueOf(years));
        monthView.setText(String.valueOf(months));
        dayView.setText(String.valueOf(days));

        yearTextView.setText(years == 1 ? "year" : "years");
        monthTextView.setText(months == 1 ? "month" : "months");
        dayTextView.setText(days == 1 ? "day" : "days");
    }

    private void setQuote() {
        quoteView.setText("Rule 10 Greed is eternal!");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_SHARED && resultCode == RESULT_OK && data != null) {
            Intent intent = new Intent(getContext(), CactusCalculatorActivity.class);
            intent.putParcelableArrayListExtra("chosen", data.getParcelableArrayListExtra("chosen"));
            startActivity(intent);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}