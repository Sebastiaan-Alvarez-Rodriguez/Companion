package com.python.companion.ui.notes.note.activity.settings.port;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.python.companion.util.export.ExportInterface;
import com.python.companion.util.export.ExportUtil;

public class ExportActivity extends PortActivity implements ExportInterface {
    private boolean skipSecure;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        infoView.setText("Prepare your settings and press start to begin exporting.");
        check.setText("Skip secure notes");
        check.setChecked(false);
        start.setOnClickListener(v -> {
            infoView.setText("We begin exporting now...");
            check.setEnabled(false);
            start.setEnabled(false);
            skipSecure = check.isChecked();

            ExportUtil.exportDatabase(this, location, skipSecure, this);
        });

        check.setOnCheckedChangeListener((buttonView, isChecked) -> barView1.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE));
    }

    @Override
    public void onStartDecryptNotes(int amount) {
        runOnUiThread(() -> {
            infoView.setText("We now decrypt secure notes. Please provide your fingerprint for each of the "+amount+ "items. (This is inconvenient, but we use a different cryptographic key for every note for high security, and every key needs authentication)");
            barState1.setText("0/"+amount);
            bar1.setMax(amount);
        });
    }

    @Override
    public void onNoteDecryptProcessed(int complete, int amount) {
        runOnUiThread(() -> {
            barState1.setText(complete + "/" + amount);
            if (complete == amount)
                infoView.setText("");
            bar1.setProgress(complete, true);
        });
    }

    @Override
    public void onNoteDecryptFinished(int amount) {
        runOnUiThread(() -> {
            if (amount == 0) {
                bar1.setMax(1);
                bar1.setProgress(1, true);
            }
        });
    }

    @Override
    public void onStartExportNotes(int amount) {
        runOnUiThread(() -> {
            infoView.setText("Processing notes...");
            barState2.setText("0/" + amount);
            bar2.setMax(amount);
        });
    }

    @Override
    public void onNoteProcessed(int complete, int failed, int amount) {
        runOnUiThread(() -> {
            barState2.setText(complete + "/" + amount);
            bar2.setProgress(complete, true);
        });
    }

    @Override
    public void onStartExportCategories(int amount) {
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
    public void onExportComplete() {
//        finish();
    }
}
