package com.python.companion.ui.note.activity.edit;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;

public class NotePreviewActivity extends AppCompatActivity {
    private TextView contentView;

    private String noteName, noteContent, prevNoteName;
    private boolean editMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);

        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("content") || !intent.hasExtra("name"))
            throw new RuntimeException("Viewer called without content!");
        noteName = intent.getStringExtra("name");
        noteContent = intent.getStringExtra("content");
        editMode = intent.hasExtra("prevName");
        if (editMode)
            prevNoteName = intent.getStringExtra("prevName");

        findViews();
        setContent();
        setupActionBar();
    }

    private void findViews() {
        contentView = findViewById(R.id.activity_note_view_content);
    }

    private void setContent() {
        boolean uselatex = true;
        if (uselatex) {
            final Markwon markwon = Markwon.builder(this).usePlugin(JLatexMathPlugin.create(10)).build();
            final Spanned markdown = markwon.toMarkdown(noteContent);
            contentView.setText(markdown);
        } else {
            final Markwon markwon = Markwon.create(this);
            final Spanned markdown = markwon.toMarkdown(noteContent);
            contentView.setText(markdown);
        }
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_note_view_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
            Intent intent = getIntent();
            String name = intent.getStringExtra("name");
            if (!intent.hasExtra("name"))
                throw new RuntimeException("Viewer called without name!");
            //TODO: Markdown title support?
            if (name.equals(""))
                name = "<no name set>";
            actionbar.setTitle(name);
        }
    }

    private void save() {
        if (noteName.length() == 0)
            Snackbar.make(findViewById(R.id.activity_note_view_layout), "Cannot save: No name for note!", Snackbar.LENGTH_LONG).show();
        else if (editMode)
            checkEdit();
        else
            checkNew();
        //TODO: Create edit support
    }

    private void checkNew() {
        Log.i("Save", "Checking new mode");
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.isUnique(noteName, unique -> {
            if (unique) {
                Log.i("Save", "New & unique. Inserting...");
                insertNew();
            } else {
                //TODO: show override dialog
            }
        });
    }

    private void checkEdit() {
        Log.i("Save", "Checking edit mode");

        boolean unchangedName = noteName.equals(prevNoteName);
        if (unchangedName) {
            Log.i("Save", "Edit & name unchanged. Updating...");
            updateExisting();
        } else {
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.isUnique(noteName, unique -> {
                if (unique) {
                    Log.i("Save", "Edit & name changed ("+prevNoteName+"->"+noteName+") & unique. Updating...");
                    updateExisting();
                } else {
                    Log.i("Save", "Edit & name changed ("+prevNoteName+"->"+noteName+") & conflict. Dialog...");
                    //TODO: show override dialog
                }
            });
        }
    }

    private void insertNew() {
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.insert(noteName, noteContent, v -> finishSuccess());
    }

    private void updateExisting() {
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.update(prevNoteName, noteName, noteContent, v -> finishSuccess());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_preview_save:
                Log.i("Save", "Save clicked");
                save();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishSuccess() {
        Intent result = new Intent();
        result.putExtra("content", noteContent);
        setResult(RESULT_OK, result);
        finish();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preview, menu);
        return true;
    }
}
