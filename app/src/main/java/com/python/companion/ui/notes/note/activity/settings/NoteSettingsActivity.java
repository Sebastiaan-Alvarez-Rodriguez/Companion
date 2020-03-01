package com.python.companion.ui.notes.note.activity.settings;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.python.companion.R;
import com.python.companion.ui.notes.note.activity.settings.port.ExportActivity;
import com.python.companion.ui.notes.note.activity.settings.port.ImportActivity;

public class NoteSettingsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EXPORT = 1;
    private static final int REQUEST_CODE_IMPORT = 2;

    private View importView, exportView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_settings);
        findGlobalViews();
        setupClicks();
        setupActionBar();
    }

    private void findGlobalViews() {
        importView = findViewById(R.id.activity_note_settings_import);
        exportView = findViewById(R.id.activity_note_settings_export);
    }

    private void setupClicks() {
        importView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent finalIntent = Intent.createChooser(intent, "Select file to import from");
            startActivityForResult(finalIntent, REQUEST_CODE_IMPORT);
        });
        exportView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            Intent finalIntent = Intent.createChooser(intent, "Select location to export to");
            startActivityForResult(finalIntent, REQUEST_CODE_EXPORT);
        });
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_note_settings_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle("Categories");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_EXPORT && resultCode == RESULT_OK && data != null) {
            Uri userChosenUri = data.getData();
            assert userChosenUri != null;
            Intent intent = new Intent(this, ExportActivity.class);
            intent.putExtra("uri", userChosenUri);
            startActivity(intent);
        } else if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK && data != null) {
            Uri userChosenUri = data.getData();
            assert userChosenUri != null;
            Intent intent = new Intent(this, ImportActivity.class);
            intent.putExtra("uri", userChosenUri);
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
