package com.python.companion.ui.note.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.ui.note.activity.edit.note.NoteEditActivity;
import com.python.companion.ui.note.activity.edit.category.CategoryEditActivity;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;

public class NoteViewActivity extends AppCompatActivity {
    private static final int REQUEST_EDIT = 1;
    private TextView contentView;

    private String name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        Intent intent = getIntent();
        name = intent.getStringExtra("name");

        String content = intent.getStringExtra("content");

        findViews();
        setContent(content);
        setupActionBar();
    }

    private void findViews() {
        contentView = findViewById(R.id.activity_note_view_content);
    }

    private void setContent(String content) {
        boolean uselatex = true;
        if (uselatex) {
            final Markwon markwon = Markwon.builder(this).usePlugin(JLatexMathPlugin.create(10)).build();
            final Spanned markdown = markwon.toMarkdown(content);
            contentView.setText(markdown);
        } else {
            final Markwon markwon = Markwon.create(this);
            final Spanned markdown = markwon.toMarkdown(content);
            contentView.setText(markdown);
        }
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_category_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
            //TODO: Markdown title support?
            actionbar.setTitle(name);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.menu_view_lock:
                //TODO: Are you sure you wish to (un)lock? (in dialog?) (snackbar afterwards)
                break;
            case R.id.menu_view_edit_category:
                intent = new Intent(this, CategoryEditActivity.class);
                intent.putExtra("noteName", name);
                startActivity(intent);
                break;
            case R.id.menu_view_edit:
                intent = new Intent(this, NoteEditActivity.class);
                intent.putExtra("name", name);

                startActivityForResult(intent, REQUEST_EDIT);
                break;
            case R.id.menu_view_delete:
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.delete(name, v -> finish());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_view, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK && data != null)
            setContent(data.getStringExtra("content"));
    }
}
