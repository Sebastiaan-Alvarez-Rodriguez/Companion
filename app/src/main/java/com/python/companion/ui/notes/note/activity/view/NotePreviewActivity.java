package com.python.companion.ui.notes.note.activity.view;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.backend.interact.Store;
import com.python.companion.backend.interact.StoreCallback;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.notes.category.activity.CategoryEditActivity;
import com.python.companion.ui.notes.note.NoteContainer;
import com.python.companion.ui.notes.note.NoteType;
import com.python.companion.util.RenderUtil;

import static com.python.companion.ui.notes.note.NoteType.TYPE_MARKDOWN;
import static com.python.companion.ui.notes.note.NoteType.TYPE_MARKDOWN_LATEX;
import static com.python.companion.ui.notes.note.NoteType.TYPE_NORMAL;

public class NotePreviewActivity extends AppCompatActivity {
    private static final int REQ_CATEGORY_EDIT = 1;

    private TextView contentView;
    private MenuItem lockItem, categoryItem, favoriteItem, typeItem;

    private Note note;

    private String prevName;
    private boolean editMode;

    private ActionBar actionbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        note = ((NoteContainer) intent.getParcelableExtra("note")).getNote();
        editMode = intent.hasExtra("prevName");
        if (editMode)
            prevName = intent.getStringExtra("prevName");
        setContentView(R.layout.activity_note_preview);
        findViews();
        setupActionBar();
        setContent(note.getContent(), note.getType());
    }


    private void findViews() {
        contentView = findViewById(R.id.activity_note_preview_content);
    }

    private void setContent(@NonNull String content, @NoteType.Type int type) {
        RenderUtil.render(contentView, content, type);
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_note_preview_toolbar);
        setSupportActionBar(myToolbar);

        actionbar = getSupportActionBar();

        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(new BlendModeColorFilter(getResources().getColor(R.color.colorWindowBackground, null), BlendMode.SRC_IN));
                myToolbar.setNavigationIcon(icon);
            }
        }
    }

    private void save() {
        if (note.getName().length() == 0) {
            Snackbar.make(findViewById(R.id.activity_note_preview_layout), "Cannot save: No name for note!", Snackbar.LENGTH_LONG).show();
        } else {
            if (editMode)
                Store.update(note, prevName, getSupportFragmentManager(), getApplicationContext(), new StoreCallback() {
                    @Override
                    public void onSuccess() {
                        finishSuccess();
                    }
                    @Override
                    public void onFailure() {

                    }
                });
            else
                Store.insert(note, getSupportFragmentManager(), getApplicationContext(), new StoreCallback() {
                    @Override
                    public void onSuccess() {
                        finishSuccess();
                    }
                    @Override
                    public void onFailure() {

                    }
                });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_preview, menu);
        lockItem = menu.findItem(R.id.menu_note_preview_lock);
        categoryItem = menu.findItem(R.id.menu_note_preview_category);
        favoriteItem = menu.findItem(R.id.menu_note_preview_favorite);
        typeItem = menu.findItem(R.id.menu_note_preview_type);

        switch (note.getType()) {
            case TYPE_NORMAL:
                menu.findItem(R.id.menu_note_preview_type_normal).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_note);
                break;
            case NoteType.TYPE_MARKDOWN:
                menu.findItem(R.id.menu_note_preview_type_markdown).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_md);
                break;
            case NoteType.TYPE_MARKDOWN_LATEX:
                menu.findItem(R.id.menu_note_preview_type_latex).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_latex);
                break;
        }

        lockItem.setIcon(note.isSecure() ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline); //getDrawable(
        favoriteItem.setIcon(note.isFavorite() ? R.drawable.ic_cactus_filled : R.drawable.ic_cactus_outline);
        actionbar.setTitle(note.getName().length() == 0 ? "<no name set>" : note.getName());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_note_preview_category:
                Intent intent = new Intent(this, CategoryEditActivity.class);
                intent.putExtra("categoryName", note.getCategory().getCategoryName());
                intent.putExtra("categoryColor", note.getCategory().getCategoryColor());
                startActivityForResult(intent, REQ_CATEGORY_EDIT);
                break;
            case R.id.menu_note_preview_lock:
                note.setSecure(!note.isSecure());
                lockItem.setIcon(getDrawable(note.isSecure() ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline));
                break;
            case R.id.menu_note_preview_favorite:
                note.setFavorite(!note.isFavorite());
                favoriteItem.setIcon(note.isFavorite() ? R.drawable.ic_cactus_filled : R.drawable.ic_cactus_outline);
                break;
            case R.id.menu_note_preview_save:
                save();
                break;
            case R.id.menu_note_preview_type:
                break;
            default:
                switch (item.getItemId()) {
                    case R.id.menu_note_preview_type_normal:
                        note.setType(TYPE_NORMAL);
                        typeItem.setIcon(R.drawable.ic_menu_note);
                        break;
                    case R.id.menu_note_preview_type_markdown:
                        note.setType(TYPE_MARKDOWN);
                        typeItem.setIcon(R.drawable.ic_menu_md);
                        break;
                    case R.id.menu_note_preview_type_latex:
                        typeItem.setIcon(R.drawable.ic_menu_latex);
                        note.setType(TYPE_MARKDOWN_LATEX);
                        break;
                }
                item.setChecked(true);
                setContent(note.getContent(), note.getType());
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishSuccess() {
        Intent result = new Intent();
        result.putExtra("note", new NoteContainer(note));
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CATEGORY_EDIT && resultCode == RESULT_OK && data != null) {
            Category c = new Category(data.getStringExtra("categoryName"), data.getIntExtra("categoryColor", -1));
            double diff = ColorUtils.calculateContrast(c.getCategoryColor(), getColor(R.color.colorPrimary));
            int displaycolor = c.getCategoryColor();
            if (diff < 4.0) {
                displaycolor = getColor(R.color.colorWindowBackground);
            }
            categoryItem.getIcon().setColorFilter(new BlendModeColorFilter(displaycolor, BlendMode.SRC_IN));
            note.setCategory(c);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
