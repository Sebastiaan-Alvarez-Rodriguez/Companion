package com.python.companion.ui.note.activity.view;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.note.NoteType;
import com.python.companion.ui.note.activity.edit.category.CategoryEditActivity;
import com.python.companion.ui.note.dialog.NoteOverrideDialog;
import com.python.companion.ui.note.dialog.lock.LockDialog;
import com.python.companion.ui.templates.dialog.DialogAcceptListener;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;

public class NotePreviewActivity extends AppCompatActivity {
    private static final int REQ_CATEGORY_EDIT = 1;

    private TextView contentView;

    private MenuItem lockItem;
    private MenuItem categoryItem;
    private MenuItem typeItem;

    private String curName, curContent, prevName;
    private Category curCategory;
    private @NoteType.Type int curType;
    private boolean curSecure;
    private ActionBar actionbar;

    private boolean editMode;
    private NotePreviewViewModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_preview);

        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("content") || !intent.hasExtra("name")) {
            Log.e("Preview", "Received no correct intent");
            throw new RuntimeException("Viewer called without content!");
        }
        curName = null;
        curContent = null;
        curCategory = null;
        curSecure = false;

        editMode = intent.hasExtra("prevName");
        if (editMode) {
            prevName = intent.getStringExtra("prevName");
            model = new ViewModelProvider(this).get(NotePreviewViewModel.class);
        }

        findViews();
        setupActionBar();
        if (!editMode) {
            curCategory = new Category("<default>", ContextCompat.getColor(this, R.color.colorPrimary));
            update(intent.getStringExtra("name"), intent.getStringExtra("content"), curCategory, curSecure, curType);
        }
    }

    private void findViews() {
        contentView = findViewById(R.id.activity_note_preview_content);
    }

    private void setContent(@NonNull String content, @NoteType.Type int type) {
        switch (type) {
            case NoteType.TYPE_NORMAL: {
                contentView.setText(curContent);
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

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_note_preview_toolbar);
        setSupportActionBar(myToolbar);

        actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
        }
    }

    private void save() {
        if (curName.length() == 0)
            Snackbar.make(findViewById(R.id.activity_note_preview_layout), "Cannot save: No name for note!", Snackbar.LENGTH_LONG).show();
        else if (editMode)
            checkEdit();
        else
            checkNew();
    }

    private void checkNew() {
        Log.i("Save", "Checking new mode");
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.isUniqueInstanced(curName, other -> {
            if (other == null) {
                Log.i("Save", "New & unique. Inserting...");
                insertNew();
            } else {
                Log.i("Save", "New & conflict. CategoryDialog...");
                showOverrideDialog(other, this::updateExisting);
            }
        });
    }

    private void checkEdit() {
        Log.i("Save", "Checking edit mode");

        boolean unchangedName = curName.equals(prevName);
        if (unchangedName) {
            Log.i("Save", "Edit & name unchanged. Updating...");
            updateExisting();
        } else {
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.isUniqueInstanced(curName, other -> {
                if (other == null) {
                    Log.i("Save", "Edit & name changed ("+ prevName +"->"+ curName +") & unique. Updating...");
                    updateExisting();
                } else {
                    Log.i("Save", "Edit & name changed ("+ prevName +"->"+ curName +") & conflict. CategoryDialog...");
                    showOverrideDialog(other, () -> noteQuery.delete(other.getName(), v -> updateExisting()));
                }
            });
        }
    }

    private void insertNew() {
        Log.e("Preview", "Store new, name: "+curName+", len(content): "+curContent.length()+", type: "+curType+", category == null: "+(curCategory==null)+"secure: "+curSecure);
        if (curSecure) {
            LockDialog dialog = new LockDialog.Builder()
                    .setAcceptListener(note -> {
                        NoteQuery noteQuery = new NoteQuery(this);
                        noteQuery.insert(note);
                        finishSuccess();
                    })
                    .setNote(new Note(curName, curContent, curCategory, false, null, curType))
                    .build();
            dialog.show(getSupportFragmentManager(), null);
        } else {
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.insert(new Note(curName, curContent, curCategory, false, null, curType));
            finishSuccess();
        }
    }

    private void updateExisting() {
        final NoteQuery noteQuery = new NoteQuery(this);
        if (editMode) { //updateContent-conflict situation
            if (curSecure) {
                LockDialog dialog = new LockDialog.Builder()
                        .setAcceptListener(note -> noteQuery.replace(prevName, note, v -> finishSuccess()))
                        .setNote(new Note(curName, curContent, curCategory, false, null, curType))
                        .build();
                dialog.show(getSupportFragmentManager(), null);
            } else {
                noteQuery.replace(prevName, new Note(curName, curContent, curCategory, false, null, curType), v -> finishSuccess());
            }
        } else { //new-conflict situation (no prevNoteName available)
            if (curSecure) {
                LockDialog dialog = new LockDialog.Builder()
                        .setAcceptListener(note -> noteQuery.update(note, v -> finishSuccess()))
                        .setNote(new Note(curName, curContent, curCategory, false, null, curType))
                        .build();
                dialog.show(getSupportFragmentManager(), null);
            } else {
                noteQuery.update(new Note(curName, curContent, curCategory, false, null, curType), v -> finishSuccess());
            }
        }
    }

    private void showOverrideDialog(Note conflicting, DialogAcceptListener overrideListener) {
        NoteOverrideDialog noteOverrideDialog = new NoteOverrideDialog.Builder()
                .setExistsText("Note name already exists!")
                .setQuestionText("Do you want to override existing note?")
                .setWarningText("Warning: Overriden notes cannot be restored")
                .setNote(conflicting)
                .setOverrideListener(overrideListener)
                .build();
        runOnUiThread(() -> noteOverrideDialog.show(getSupportFragmentManager(), null));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_note_preview_category:
                Intent intent = new Intent(this, CategoryEditActivity.class);
                if (curCategory != null) {
                    intent.putExtra("categoryName", curCategory.getCategoryName());
                    intent.putExtra("categoryColor", curCategory.getCategoryColor());
                }
                startActivityForResult(intent, REQ_CATEGORY_EDIT);
                break;
            case R.id.menu_note_preview_type_normal:
                update(curName, curContent, curCategory, curSecure, NoteType.TYPE_NORMAL);
                break;
            case R.id.menu_note_preview_type_markdown:
                update(curName, curContent, curCategory, curSecure, NoteType.TYPE_MARKDOWN);
                break;
            case R.id.menu_note_preview_type_latex:
                update(curName, curContent, curCategory, curSecure, NoteType.TYPE_MARKDOWN_LATEX);
                break;
            case R.id.menu_note_preview_save:
                save();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_preview, menu);
        lockItem = menu.findItem(R.id.menu_note_preview_lock);
        categoryItem = menu.findItem(R.id.menu_note_preview_category);
        typeItem = menu.findItem(R.id.menu_note_preview_type);
        if (editMode)
            model.getNote(curName).observe(this, note -> update(note.getName(), note.getContent(), note.getCategory(), note.isSecure(), note.getType()));
        return true;
    }

    private void update(@NonNull String name, @NonNull String content, @NonNull Category category, boolean secure, @NoteType.Type int type) {
        Log.e("Preview", "Update received: "+name+" : "+content+ " : ");
        if (!name.equals(curName)) {
            curName = name;
            if (curName.equals(""))
                curName = "<no name set>";
            actionbar.setTitle(name);
        }

        if (!content.equals(curContent)) {
            curContent = content;
            if (type == curType)
                setContent(curContent, type);
        }

        if (category != curCategory) {
            int categoryColor = category.getCategoryName().length() == 0 ? getColor(R.color.colorPrimary) : category.getCategoryColor();
            double diff = ColorUtils.calculateContrast(categoryColor, getColor(R.color.colorPrimary));
            if (diff < 1.3)
                categoryColor = getColor(R.color.colorWindowBackground);
            categoryItem.getIcon().setColorFilter(categoryColor, PorterDuff.Mode.SRC_IN);
            curCategory = category;
        }

        if (type != curType) {
            setContent(curContent, type);
            curType = type;
            Drawable d;
            switch (type) {
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
            typeItem.setIcon(d);
        }

        if (secure != curSecure) {
            lockItem.setIcon(getDrawable(secure ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline));
            curSecure = secure;
        }
    }

    private void finishSuccess() {
        Intent result = new Intent();
        result.putExtra("content", curContent);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CATEGORY_EDIT && resultCode == RESULT_OK && data != null) {
            Log.i("Preview", "Received intent from category edit");
            Category tmp = new Category(data.getStringExtra("categoryName"), data.getIntExtra("categoryColor", -1));
            update(curName, curContent, tmp, curSecure, curType);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
