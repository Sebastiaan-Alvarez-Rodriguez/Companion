package com.python.companion.ui.notes.note.activity.settings.port;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.python.companion.util.jnport.ImportInterface;
import com.python.companion.util.jnport.ImportUtil;

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
            Uri location = getIntent().getData();
            assert location != null;
            ImportUtil.importDatabase(this, location, reSecure, this);
        });

        check.setOnCheckedChangeListener((buttonView, isChecked) -> barView1.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE));
    }

    @Override
    public void onStartEncryptNotes(int amount) {
        infoView.setText("We now re-secure previously secured notes. Please provide your fingerprint for each of the "+amount+ "items. (This is inconvenient, but we use a different cryptographic key for every note for high security, and every key needs authentication)");
        barState1.setText("0/"+amount);
        bar1.setMax(amount);
    }

    @Override
    public void onNoteEncryptProcessed(int complete, int amount) {
        barState1.setText(complete+"/"+amount);
        if (complete == amount)
            infoView.setText("");
        bar1.setProgress(complete, true);
    }

    @Override
    public void onStartImportNotes(int complete, int amount) {
        infoView.setText(reSecure ? "Processing remaining notes..." : "Processing notes...");
        barState2.setText(complete+"/"+amount);
        bar2.setMin(complete);
        bar2.setMax(amount);
    }

    @Override
    public void onNoteProcessed(int complete, int failed, int amount) {
        barState2.setText(complete+"/"+amount);
        bar2.setProgress(complete, true);
    }

    @Override
    public void onStartImportCategories(int amount) {
        infoView.setText("Processing categories...");
        barState3.setText("0/"+amount);
        bar3.setMax(amount);
    }

    @Override
    public void onCategoryProcessed(int complete, int failed, int amount) {
        barState3.setText(complete+"/"+amount);
        bar3.setProgress(complete, true);
    }

    @Override
    public void onImportComplete(int complete, int failed, int amount) {
        finish();
    }
}
