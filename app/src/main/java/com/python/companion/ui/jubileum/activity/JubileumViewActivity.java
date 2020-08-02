package com.python.companion.ui.jubileum.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.jubileum.MeasurementContainer;
import com.python.companion.ui.jubileum.activity.calculate.JubileumCalculatorActivity;
import com.python.companion.ui.jubileum.activity.calculate.JubileumCalculatorSharedActivity;
import com.python.companion.util.MeasurementUtil;
import com.python.companion.util.ThreadUtil;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.Executors;

public class JubileumViewActivity extends AppCompatActivity {
    private int REQ_EDIT = 1, REQ_SELECT = 2;

    private View layout;
    private TextView equationView, amountHadView, amountHadNameView, nextAnnounceView, nextDateView, nextDistanceView, notificationsAnnounceView;
    private ListView notificationsView;
    private TextView addNotificationView;
    private FloatingActionButton editButton, calculatorButton, calculatorSharedButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jubileum_view);
        findViews();

        Intent intent = getIntent();
        Measurement measurement = ((MeasurementContainer) Objects.requireNonNull(intent.getParcelableExtra("measurement"))).getMeasurement();

        String parentSingular = intent.getStringExtra("parentSingular");
        String parentPlural = intent.getStringExtra("parentPlural");

        setupActionBar(measurement.getNamePlural());
        setMeasurement(measurement, Objects.requireNonNull(parentSingular), Objects.requireNonNull(parentPlural));
        prepareButtons(measurement);
    }

    private void findViews() {
        layout = findViewById(R.id.activity_jubileum_view_layout);
        equationView = findViewById(R.id.activity_jubileum_view_equation);
        amountHadView = findViewById(R.id.activity_jubileum_view_amount_had);
        amountHadNameView = findViewById(R.id.activity_jubileum_view_amount_had_name);
        nextAnnounceView = findViewById(R.id.activity_jubileum_view_next_announce);
        nextDateView = findViewById(R.id.activity_jubileum_view_next_date);
        nextDistanceView = findViewById(R.id.activity_jubileum_view_next_distance);
        notificationsAnnounceView = findViewById(R.id.activity_jubileum_view_notifications_announce);
        notificationsView = findViewById(R.id.activity_jubileum_view_notifications);
        addNotificationView = findViewById(R.id.activity_jubileum_view_notifications_add2);

        editButton = findViewById(R.id.activity_jubileum_view_edit);
        calculatorButton = findViewById(R.id.activity_jubileum_view_jcalculator);
        calculatorSharedButton = findViewById(R.id.activity_jubileum_view_jcalculator_shared);
    }

    private void setupActionBar(@NonNull String title) {
        Toolbar myToolbar = findViewById(R.id.activity_jubileum_view_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(title);
        }
    }

    private void setMeasurement(@NonNull Measurement measurement, @NonNull String parentSingular, @NonNull String parentPlural) {
        equationView.setText("1 "+measurement.getNameSingular()+" = "+measurement.getAmount()+" "+(measurement.getAmount() == 1 ? parentSingular : parentPlural));
        LocalDate together = LocalDate.parse(getSharedPreferences(getString(R.string.cactus_preferences), MODE_PRIVATE).getString(getString(R.string.cactus_preferences_key_together), "2017-11-08"));
        Executors.newSingleThreadExecutor().execute(() -> {
            long jubileaHad = MeasurementUtil.distanceCurrent(measurement, together);
            LocalDate nextJubileum = MeasurementUtil.futureInterval(measurement, together, 1);
            long daysToNext = MeasurementUtil.computeDistance(nextJubileum);
            ThreadUtil.runOnUIThread(()-> {
                amountHadView.setText(String.valueOf(jubileaHad));
                nextAnnounceView.setText("Your "+(jubileaHad+1)+MeasurementUtil.getDayOfMonthSuffix((int) (jubileaHad+1))+" jubileum will be on");
                nextDateView.setText(nextJubileum.toString());
                nextDistanceView.setText("which is "+daysToNext+(daysToNext == 1 ? " day" : " days")+" from now");
                amountHadNameView.setText(measurement.getNameSingular() + ((jubileaHad == 1) ? " jubileum" : " jubilea"));
            });
        });
    }

    private void prepareButtons(@NonNull Measurement measurement) {
        editButton.setOnClickListener(v -> {
            if (MeasurementUtil.isDefault(measurement.getMeasurementID())) {
                Snackbar.make(layout, "You cannot edit this default type!", Snackbar.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, JubileumEditActivity.class);
            intent.putExtra("measurement", new MeasurementContainer(measurement));
            startActivityForResult(intent, REQ_EDIT);
        });

        calculatorButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, JubileumCalculatorActivity.class);
            startActivity(intent);
        });

        calculatorSharedButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, JubileumSelectActivity.class);
            startActivityForResult(intent, REQ_SELECT);
        });

        addNotificationView.setOnClickListener(v -> {
            Log.e("JubileumViewActivity", "Clicked add text");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK) {
            finish();
            return;
        } else if (requestCode == REQ_SELECT) {
            Intent intent = new Intent(this, JubileumCalculatorSharedActivity.class);
            intent.putParcelableArrayListExtra("chosen", data.getParcelableArrayListExtra("chosen"));
            startActivity(intent);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
