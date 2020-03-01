package com.python.companion.ui.notes.note.activity.settings.port;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.python.companion.R;

public abstract class PortActivity extends AppCompatActivity {
    protected View barView1, barView2, barView3;
    protected ProgressBar bar1, bar2, bar3;
    protected TextView barState1, barState2, barState3, infoView, warningView;
    protected CheckBox check;
    protected Button start;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_port);
        findGlobalViews();
    }

    protected void findGlobalViews() {
        barView1 = findViewById(R.id.activity_port_barview1);
        barView2 = findViewById(R.id.activity_port_barview2);
        barView3 = findViewById(R.id.activity_port_barview3);

        bar1 = barView1.findViewById(R.id.activity_port_bar1);
        bar2 = barView2.findViewById(R.id.activity_port_bar2);
        bar3 = barView3.findViewById(R.id.activity_port_bar3);

        barState1 = barState1.findViewById(R.id.activity_port_bar1_state);
        barState2 = barState2.findViewById(R.id.activity_port_bar2_state);
        barState3 = barState3.findViewById(R.id.activity_port_bar3_state);

        infoView = findViewById(R.id.activity_port_info);
        warningView = findViewById(R.id.activity_port_warning);
        check = findViewById(R.id.activity_port_check);
        start = findViewById(R.id.activity_port_start);
    }
}
