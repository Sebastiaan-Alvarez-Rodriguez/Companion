package com.python.companion.ui.settings.port;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.util.migration.MigrationInterface;

public abstract class PortActivity extends AppCompatActivity implements MigrationInterface {
    protected View layout, barView1, barView2, barView3;
    protected ProgressBar bar1, bar2, bar3;
    protected TextView barState1, barState2, barState3, barStateMax1, barStateMax2, barStateMax3, infoView, warningView;
    protected CheckBox check;
    protected Button start;

    protected Uri location;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_port);
        findGlobalViews();
        location = getIntent().getParcelableExtra("uri");
    }

    protected void findGlobalViews() {
        layout = findViewById(R.id.activity_port_layout);

        barView1 = findViewById(R.id.activity_port_barview1);
        barView2 = findViewById(R.id.activity_port_barview2);
        barView3 = findViewById(R.id.activity_port_barview3);

        bar1 = barView1.findViewById(R.id.activity_port_bar1);
        bar2 = barView2.findViewById(R.id.activity_port_bar2);
        bar3 = barView3.findViewById(R.id.activity_port_bar3);

        barState1 = barView1.findViewById(R.id.activity_port_bar1_state);
        barState2 = barView2.findViewById(R.id.activity_port_bar2_state);
        barState3 = barView3.findViewById(R.id.activity_port_bar3_state);

        barStateMax1 = barView1.findViewById(R.id.activity_port_bar1_state_max);
        barStateMax2 = barView2.findViewById(R.id.activity_port_bar2_state_max);
        barStateMax3 = barView3.findViewById(R.id.activity_port_bar3_state_max);

        infoView = findViewById(R.id.activity_port_info);
        warningView = findViewById(R.id.activity_port_warning);
        check = findViewById(R.id.activity_port_check);
        start = findViewById(R.id.activity_port_start);
    }

    private long notesAmount, secureNotesAmount;
    private int complete, failed;
    @Override
    @CallSuper
    public void onStatsAvailable(long categoryAmount, long notesAmount, long secureNotesAmount, long measurementAmount) {
        if (categoryAmount != 0) {
            barState1.setText("0");
            barStateMax1.setText(String.valueOf(categoryAmount));
            bar1.setMax((int) categoryAmount);
        } else {
            barState1.setText("-");
            barStateMax1.setText("-");
        }

        if (notesAmount != 0) {
            barState2.setText("0");
            barStateMax2.setText(String.valueOf(notesAmount));
            bar2.setMax((int) notesAmount);
        } else {
            barState2.setText("-");
            barStateMax2.setText("-");
        }

        if (measurementAmount != 0) {
            barState3.setText("0");
            barStateMax3.setText(String.valueOf(measurementAmount));
            bar3.setMax((int) measurementAmount);
        } else {
            barState3.setText("-");
            barStateMax3.setText("-");
        }

        this.notesAmount = notesAmount;
        this.secureNotesAmount = secureNotesAmount;
    }

    @Override
    @CallSuper
    public void onStartCategories() {
        infoView.setText("Processing categories...");
        complete = failed = 0;
    }

    @Override
    @CallSuper
    public void onCategoryProcessed() {
        ++complete;
        barState1.setText(String.valueOf(complete));
        bar1.setProgress(complete, true);
    }

    @Override
    @CallSuper
    public void onCategoryFailed() {
        ++failed;
    }

    @Override
    public void onFinishCategories() {
        infoView.setText("");
    }

    @Override
    @CallSuper
    public void onStartNotes() {
        complete = failed = 0;
        infoView.setText("Processing "+notesAmount+" notes...");
    }

    @Override
    public void onNoteProcessed() {
        ++complete;
        barState2.setText(String.valueOf(complete));
        bar2.setProgress(complete, true);
    }

    @Override
    public void onNoteFailed() {
        ++failed;
    }

    @Override
    public void onFinishNotes() {
        infoView.setText("");
    }

    @Override
    public void onStartMeasurements() {
        complete = failed = 0;
        infoView.setText("Processing measurements...");
    }

    @Override
    public void onMeasurementProcessed() {
        ++complete;
        barState3.setText(String.valueOf(complete));
        bar3.setProgress(complete, true);
    }

    @Override
    public void onMeasurementFailed() {
        ++failed;
    }

    @Override
    public void onFinishMigration() {
        infoView.setText("Success!");
        start.setText("OK");
        start.setEnabled(true);
    }

    @Override
    public void onFatalError(@NonNull String error) {
        Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show();
        infoView.setText("Error encountered");
        start.setText("Retry");
        start.setEnabled(true);
        check.setEnabled(true);
    }

    @Override
    public void onFinishMeasurements() {
        infoView.setText("");
    }
}
