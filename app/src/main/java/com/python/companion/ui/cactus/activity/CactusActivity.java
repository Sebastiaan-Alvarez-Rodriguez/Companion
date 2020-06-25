package com.python.companion.ui.cactus.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.python.companion.R;
import com.python.companion.ui.cactus.activity.measurement.MeasurementSelectActivity;

public class CactusActivity extends AppCompatActivity {
    private static final int REQ_SHARED = 0;

    private Button jubileumButton;
    private Button sharedButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cactus);
        findGlobalViews();
        setupClicks();
        setupActionBar();
    }

    private void findGlobalViews() {
        jubileumButton = findViewById(R.id.activity_cactus_jubilea);
        sharedButton = findViewById(R.id.activity_cactus_jubilea_shared);
    }

    private void setupClicks() {
        jubileumButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CactusJubileumActivity.class);
            startActivity(intent);
        });

        sharedButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MeasurementSelectActivity.class);
            startActivityForResult(intent, REQ_SHARED);
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_cactus_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_SHARED && resultCode == RESULT_OK && data != null) {
            Intent intent = new Intent(this, CactusDistanceActivity.class);
            intent.putParcelableArrayListExtra("chosen", data.getParcelableArrayListExtra("chosen"));
            startActivity(intent);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
