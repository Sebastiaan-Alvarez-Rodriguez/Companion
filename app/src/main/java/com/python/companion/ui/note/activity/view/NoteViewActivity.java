package com.python.companion.ui.note.activity.view;

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
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.ui.note.NoteType;
import com.python.companion.ui.note.activity.edit.category.CategoryEditActivity;
import com.python.companion.ui.note.activity.edit.note.NoteEditActivity;
import com.python.companion.ui.note.dialog.lock.LockDialog;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;

public class NoteViewActivity extends AppCompatActivity {
    private static final int REQUEST_EDIT = 1;
    private TextView contentView;
    private FloatingActionButton editButton;

    private String name;
    private String content;
    private @NoteType.Type int type;

    private NoteViewViewModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        model = new ViewModelProvider(this).get(NoteViewViewModel.class);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        content = intent.getStringExtra("content");
        type = intent.getIntExtra("type", NoteType.TYPE_NORMAL);
        findViews();
        setupButton();
        setContent(content, type);
        setupActionBar();
    }

    private void findViews() {
        contentView = findViewById(R.id.activity_note_view_content);
        editButton = findViewById(R.id.activity_note_view_edit);
    }

    private void setupButton() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("content", content);
            startActivityForResult(intent, REQUEST_EDIT);
        });
    }

    private void setContent(String content, @NoteType.Type int type) {
        switch (type) {
            case NoteType.TYPE_NORMAL: {
                contentView.setText(content);
                break;
            }
            case NoteType.TYPE_MARKDOWN: {
                final Markwon markwon = Markwon.create(this);
                final Spanned markdown = markwon.toMarkdown(content);
                contentView.setText(markdown);
                break;
            }
            case NoteType.TYPE_MARKDOWN_LATEX: {
                final Markwon markwon = Markwon.builder(this).usePlugin(JLatexMathPlugin.create(10)).build();
                final Spanned markdown = markwon.toMarkdown(content);
                contentView.setText(markdown);
            }
        }
    }

    private void refreshContentWithType(@NoteType.Type int type) {
        this.type = type;
        setContent(content, type);
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
            actionbar.setTitle(name);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_note_view_lock: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.get(name, note -> {
                    LockDialog dialog = new LockDialog.Builder()
                            .setAcceptListener(() -> Snackbar.make(contentView, "Successfully changed lock status!", Snackbar.LENGTH_LONG).show())
                            .setNote(note)
                            .build();
                    runOnUiThread(() -> dialog.show(getSupportFragmentManager(), null));
                });
                break;
            }
            case R.id.menu_note_view_edit_category: {
                Intent intent = new Intent(this, CategoryEditActivity.class);
                intent.putExtra("noteName", name);
                startActivity(intent);
                break;
            }
            case R.id.menu_note_view_type_normal: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.updateType(name, NoteType.TYPE_NORMAL, v -> refreshContentWithType(NoteType.TYPE_NORMAL));
                break;
            }
            case R.id.menu_note_view_type_markdown: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.updateType(name, NoteType.TYPE_MARKDOWN, v -> refreshContentWithType(NoteType.TYPE_MARKDOWN));
                break;
            }
            case R.id.menu_note_view_type_latex: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.updateType(name, NoteType.TYPE_MARKDOWN_LATEX, v -> refreshContentWithType(NoteType.TYPE_MARKDOWN_LATEX));
                break;
            }
            case R.id.menu_note_view_delete: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.delete(name, v -> finish());
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_view, menu);
        MenuItem lock = menu.findItem(R.id.menu_note_view_lock);
        MenuItem category = menu.findItem(R.id.menu_note_view_edit_category);
        MenuItem type = menu.findItem(R.id.menu_note_view_type);
        model.getNote(name).observe(this, note -> {
            lock.setIcon(getDrawable(note.isSecure() ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline));

            int categoryColor = note.getCategory().getCategoryName().length() == 0 ? getColor(R.color.colorPrimary) : note.getCategory().getCategoryColor();
            double diff = ColorUtils.calculateContrast(categoryColor, getColor(R.color.colorPrimary));
            if (diff < 1.3)
                categoryColor = getColor(R.color.colorWindowBackground);
            category.getIcon().setColorFilter(categoryColor, PorterDuff.Mode.SRC_IN);

            Drawable d;
            switch (note.getType()) {
                case NoteType.TYPE_NORMAL:
                    d = getDrawable(R.drawable.ic_menu_note);
                    break;
                case NoteType.TYPE_MARKDOWN:
                    d = getDrawable(R.drawable.ic_menu_md);
                    break;
                case NoteType.TYPE_MARKDOWN_LATEX:
                    default:
                    d = getDrawable(R.drawable.ic_menu_latex);
            }
            type.setIcon(d);
        });
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK && data != null)
            setContent(data.getStringExtra("content"), type);
    }
}
