package com.python.companion.ui.notes.note.activity.view;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.os.Bundle;
import android.text.Spannable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.db.interact.NoteStore;
import com.python.companion.ui.general.textviewsearch.UITextSearcher;
import com.python.companion.ui.notes.category.activity.CategoryEditActivity;
import com.python.companion.ui.notes.note.NoteContainer;
import com.python.companion.ui.notes.note.NoteType;
import com.python.companion.util.RenderUtil;

import static com.python.companion.ui.notes.note.NoteType.TYPE_MARKDOWN;
import static com.python.companion.ui.notes.note.NoteType.TYPE_MARKDOWN_LATEX;
import static com.python.companion.ui.notes.note.NoteType.TYPE_NORMAL;

public class NotePreviewActivity extends AppCompatActivity {
    private static final int REQ_CATEGORY_EDIT = 1;

    private CoordinatorLayout layout;
    private NestedScrollView nestedScrollView;
    private TextView contentView;
    private SearchView searchView;
    private MenuItem lockItem, categoryItem, favoriteItem, typeItem;

    private Note note;
    private boolean makeSecure;

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
        else // We have not set a category yet
            note.setCategory(new Category("<default>", ContextCompat.getColor(this, R.color.colorPrimary)));

        setContentView(R.layout.activity_note_preview);
        layout = findViewById(R.id.activity_note_preview_layout);
        findViews();
        setupActionBar();
        setContent(note.getContent(), note.getType());

        makeSecure = false;
    }

    private void findViews() {
        layout = findViewById(R.id.activity_note_preview_layout);
        nestedScrollView = findViewById(R.id.activity_note_preview_scrollview);
        contentView = nestedScrollView.findViewById(R.id.activity_note_preview_content);
    }

    private void setContent(@NonNull String content, @NoteType.Type int type) {
        RenderUtil.render(contentView, content, type);
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_note_preview_toolbar);
        setSupportActionBar(myToolbar);

        actionbar = getSupportActionBar();

        if (actionbar != null)
            actionbar.setDisplayHomeAsUpEnabled(true);
    }

    private void setSearch() {
        UITextSearcher searcher = new UITextSearcher(getSupportFragmentManager(), layout, nestedScrollView, contentView, searchView, (Spannable) contentView.getText());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searcher.submit(query, true);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                // Window opened
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                //Window closed
                searcher.finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_preview, menu);
        searchView = (SearchView) menu.findItem(R.id.menu_note_preview_search).getActionView();
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

        lockItem.setIcon(makeSecure ? R.drawable.ic_lock_full : R.drawable.ic_lock_open_outline); //getDrawable(
        favoriteItem.setIcon(note.isFavorite() ? R.drawable.ic_cactus_filled : R.drawable.ic_cactus_outline);
        actionbar.setTitle(note.getName().length() == 0 ? "<no name set>" : note.getName());
        setSearch();
        return true;
    }

    private void save() {
        if (note.getName().length() == 0) {
            Snackbar.make(layout, "Cannot save: No name for note!", Snackbar.LENGTH_LONG).show();
        } else {
            if (editMode)
                NoteStore.update(note, prevName, makeSecure, getSupportFragmentManager(), getApplicationContext(), this::finishSuccess, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show());
            else
                NoteStore.insert(note, makeSecure, getSupportFragmentManager(), getApplicationContext(), this::finishSuccess, error -> Snackbar.make(layout, error, Snackbar.LENGTH_LONG).show());
        }
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
                makeSecure = !makeSecure;
                lockItem.setIcon(getDrawable(makeSecure ? R.drawable.ic_lock_full : R.drawable.ic_lock_open_outline));
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
                displaycolor = getColor(android.R.color.transparent);
            }
            categoryItem.getIcon().setColorFilter(new BlendModeColorFilter(displaycolor, BlendMode.SRC_IN));
            note.setCategory(c);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
