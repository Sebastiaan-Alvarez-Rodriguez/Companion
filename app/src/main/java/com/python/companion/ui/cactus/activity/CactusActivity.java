package com.python.companion.ui.cactus.activity;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.python.companion.R;

public class CactusActivity extends AppCompatActivity {
    private RecyclerView list;
    private BottomAppBar bar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cactus);
        findGlobalViews();
    }

    private void findGlobalViews() {
        list = findViewById(R.id.activity_cactus_measurements);
        bar = findViewById(R.id.activity_cactus_bottombar);
    }

    private void setupActionBar() {
        setSupportActionBar(bar);
        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = bar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                bar.setNavigationIcon(icon);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.activity_cactus_menu_search:
                break;
            case R.id.activity_cactus_menu_sort:
                break;
            case R.id.activity_cactus_menu_settings:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_cactus, menu); //https://material.io/develop/android/components/bottom-app-bar/
        return true;
    }
}
