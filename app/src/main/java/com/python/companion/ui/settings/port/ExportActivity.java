package com.python.companion.ui.settings.port;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.util.migration.Exporter;

public class ExportActivity extends PortActivity {
    private boolean exporting, skipSecure, finished;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        infoView.setText("Press button to begin exporting.");
        check.setText("Skip secure notes");
        check.setChecked(false);
        exporting = false;
        skipSecure = false;
        finished = false;
        handleClicks();
    }

    private void handleClicks() {
        start.setOnClickListener(v -> {
            if (!finished) {
                check.setEnabled(false);
                exporting = true;
                infoView.setText("We begin exporting now...");
                check.setEnabled(false);
                start.setEnabled(false);
                skipSecure = check.isChecked();

                Exporter.from(getSupportFragmentManager(), this).with(this).export(location, skipSecure);
            } else {
                finish();
            }
        });
    }

    @Override
    public void onFinishMigration() {
        exporting = false;
        finished = true;
        infoView.setText("Exporting completed successfully");
        start.setText("OK");
        start.setEnabled(true);
    }

    @Override
    public void onFatalError(@NonNull String error) {
        super.onFatalError(error);
        exporting = false;
        finished = false;
    }

    @Override
    public void onBackPressed() {
        if (!exporting) // Only allow user to go back when we are not exporting
            super.onBackPressed();
    }
}
