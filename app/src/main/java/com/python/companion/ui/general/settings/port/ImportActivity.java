package com.python.companion.ui.general.settings.port;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.util.migration.MigrationInterface;
import com.python.companion.util.migration.Importer;

public class ImportActivity extends PortActivity implements MigrationInterface {
    private boolean importing, reSecure, finished;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        infoView.setText("Press button to begin importing.");
        check.setText("Re-secure previously secure notes");
        check.setChecked(true);
        importing = false;
        reSecure = true;
        finished = false;
        handleClicks();
    }

    private void handleClicks() {
        start.setOnClickListener(v -> {
            if (!finished) {
                check.setEnabled(false);
                importing = true;
                infoView.setText("We begin importing now...");
                check.setEnabled(false);
                start.setEnabled(false);
                reSecure = check.isChecked();

                Importer.from(getSupportFragmentManager(), this).with(this).jnport(location, reSecure);
            } else {
                finish();
            }
        });
    }


    @Override
    public void onFinishMigration() {
        importing = false;
        finished = true;
        infoView.setText("Importing completed successfully");
        start.setText("OK");
        start.setEnabled(true);
    }

    @Override
    public void onFatalError(@NonNull String error) {
        super.onFatalError(error);
        importing = false;
        finished = false;
    }

    @Override
    public void onBackPressed() {
        if (!importing)
            super.onBackPressed();
    }
}
