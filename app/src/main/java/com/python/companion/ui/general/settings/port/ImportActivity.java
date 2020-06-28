package com.python.companion.ui.general.settings.port;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.python.companion.util.migration.jnport.ImportInterface;
import com.python.companion.util.migration.jnport.ImportUtil;

public class ImportActivity extends PortActivity implements ImportInterface {
    private boolean reSecure;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        infoView.setText("Prepare your settings and press start to begin importing.");
        check.setText("Re-secure previously secure notes");
        check.setChecked(true);
        start.setOnClickListener(v -> {
            infoView.setText("We begin importing now...");
            check.setEnabled(false);
            start.setEnabled(false);
            reSecure = check.isChecked();
            ImportUtil.importDatabase(getSupportFragmentManager(),this, location, reSecure, this);
        });

        check.setOnCheckedChangeListener((buttonView, isChecked) -> barView2.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE));
    }

    @Override
    public void onStartImportNotes(int amount) {
        runOnUiThread(() -> {
            infoView.setText("Processing notes...");
            barState1.setText("0/" + amount);
            bar1.setMax(amount);
        });
    }

    @Override
    public void onNoteProcessed(int complete, int failed, int amount) {
        runOnUiThread(() -> {
            barState1.setText(complete + "/" + amount);
            bar1.setProgress(complete, true);
        });
    }

    @Override
    public void onStartEncryptNotes(int amount) {
        runOnUiThread(() -> {
            infoView.setText("We now re-secure previously secured notes. Please provide your fingerprint for each of the " + amount + "items. (This is inconvenient, but we use a different cryptographic key for every note for high security, and every key needs authentication)");
            barState2.setText("0/" + amount);
            bar2.setMax(amount);
        });
    }

    @Override
    public void onNoteEncryptProcessed(int complete, int amount) {
        runOnUiThread(() -> {
            barState2.setText(complete + "/" + amount);
            if (complete == amount)
                infoView.setText("");
            bar2.setProgress(complete, true);
        });
    }

    @Override
    public void onStartImportCategories(int amount) {
        runOnUiThread(() -> {
            infoView.setText("Processing categories...");
            barState3.setText("0/" + amount);
            bar3.setMax(amount);
        });
    }

    @Override
    public void onCategoryProcessed(int complete, int failed, int amount) {
        runOnUiThread(() -> {
            barState3.setText(complete + "/" + amount);
            bar3.setProgress(complete, true);
        });
    }

    @Override
    public void onImportComplete() {
        runOnUiThread(() -> {
//        finish();
        });
    }
}
