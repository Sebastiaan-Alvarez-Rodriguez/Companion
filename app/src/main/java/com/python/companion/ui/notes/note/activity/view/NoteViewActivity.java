package com.python.companion.ui.notes.note.activity.view;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.os.Bundle;
import android.text.Spannable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.ui.general.textviewsearch.UITextSearcher;
import com.python.companion.ui.notes.category.activity.CategoryEditActivity;
import com.python.companion.ui.notes.note.NoteContainer;
import com.python.companion.ui.notes.note.NoteType;
import com.python.companion.ui.notes.note.activity.edit.NoteEditActivity;
import com.python.companion.util.ColorUtil;
import com.python.companion.util.RenderUtil;
import com.python.companion.util.genericinterfaces.ResultListener;

import static com.python.companion.ui.notes.note.NoteType.TYPE_MARKDOWN_LATEX;

public class NoteViewActivity extends AppCompatActivity {
    private static final int REQ_EDIT = 1;
    private static final int REQ_CATEGORY_EDIT = 2;
    private CoordinatorLayout layout;
    private NestedScrollView nestedScrollView;
    private TextView contentView;
    private FloatingActionButton editButton;

    private Note note;
    private String text;

    private SearchView searchView;
    private MenuItem searchItem, categoryItem, favoriteItem, typeItem, lockItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        layout = findViewById(R.id.activity_note_view_layout);

        Intent intent = getIntent();
        note = ((NoteContainer) intent.getParcelableExtra("note")).getNote();
        text = note.isSecure() ? intent.getStringExtra("plaintext") : note.getContent();

        findViews();
        setupButton();
        setupActionBar();
        setContent(text, note.getType());
    }

    private void findViews() {
        nestedScrollView = findViewById(R.id.activity_note_view_scrollview);
        contentView = nestedScrollView.findViewById(R.id.activity_note_view_content);
        editButton = findViewById(R.id.activity_note_view_edit);
    }

    private void setupButton() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.putExtra("note", new NoteContainer(new Note(note.getName(), text, note.getCategory(), note.isSecure(), note.getIv(), note.getType(), note.isFavorite())));
            startActivityForResult(intent, REQ_EDIT);
        });
    }

    private void setContent(String content, @NoteType.Type int type) {
        RenderUtil.render(contentView, content, type);
    }

    private void setupActionBar() {
        Toolbar myToolbar = findViewById(R.id.activity_note_view_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(note.getName());
        }
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

    private void setCategory(int categoryColor) {
        double diff = ColorUtil.computeDiff(categoryColor, getColor(R.color.colorPrimary));
        if (Math.abs(diff) < 4.0)
            categoryColor = getColor(android.R.color.transparent);
        categoryItem.getIcon().setColorFilter(new BlendModeColorFilter(categoryColor, BlendMode.SRC_IN));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_view, menu);
        searchItem = menu.findItem(R.id.menu_note_view_search);
        searchView = (SearchView) searchItem.getActionView();
        categoryItem = menu.findItem(R.id.menu_note_view_edit_category);
        favoriteItem = menu.findItem(R.id.menu_note_view_favorite);
        typeItem = menu.findItem(R.id.menu_note_view_type);
        lockItem = menu.findItem(R.id.menu_note_view_lock);

        setCategory(note.getCategory().getCategoryColor());

        lockItem.setIcon(getDrawable(note.isSecure() ? R.drawable.ic_lock_full : R.drawable.ic_lock_open_outline));
        favoriteItem.setIcon(getDrawable(note.isFavorite() ? R.drawable.ic_cactus_filled : R.drawable.ic_cactus_outline));
        switch (note.getType()) {
            case NoteType.TYPE_MARKDOWN:
                menu.findItem(R.id.menu_note_view_type_markdown).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_md);
                break;
            case NoteType.TYPE_MARKDOWN_LATEX:
                menu.findItem(R.id.menu_note_view_type_latex).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_latex);
                searchItem.setVisible(false);
                break;
            case NoteType.TYPE_NORMAL:
            default:
                menu.findItem(R.id.menu_note_view_type_normal).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_note);
                break;
        }
        if (note.getType() != TYPE_MARKDOWN_LATEX)
            setSearch();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_note_view_lock: {
                ResultListener<Note> listener = note -> {
                    NoteQuery query = new NoteQuery(getApplicationContext());
                    query.update(note, v -> {});
                    this.note = note;
                    runOnUiThread(() -> {
                        lockItem.setIcon(getDrawable(this.note.isSecure() ? R.drawable.ic_lock_full : R.drawable.ic_lock_open_outline));
                        Snackbar.make(contentView, "Successfully changed lock status!", Snackbar.LENGTH_LONG).show();
                    });
                };
                if (note.isSecure())
                    NoteConverter.Decrypter.from(getSupportFragmentManager(), getApplicationContext())
                            .setOnErrorListener(error -> Snackbar.make(contentView, error, Snackbar.LENGTH_LONG).show())
                            .setOnFinishListener(listener)
                            .decrypt(note);
                else
                    NoteConverter.Encrypter.from(getSupportFragmentManager(), getApplicationContext())
                            .setOnErrorListener(error -> Snackbar.make(contentView, error, Snackbar.LENGTH_LONG).show())
                            .setOnFinishListener(listener)
                            .encrypt(note);
                break;
            }
            case R.id.menu_note_view_edit_category: {
                Intent intent = new Intent(this, CategoryEditActivity.class);
                intent.putExtra("categoryName", note.getCategory().getCategoryName());
                intent.putExtra("categoryColor", note.getCategory().getCategoryColor());
                startActivityForResult(intent, REQ_CATEGORY_EDIT);
                break;
            }
            case R.id.menu_note_view_favorite: {
                Log.e("View", "Favorite was "+note.isFavorite()+", now "+!note.isFavorite());
                note.setFavorite(!note.isFavorite());
                favoriteItem.setIcon(getDrawable(note.isFavorite() ? R.drawable.ic_cactus_filled : R.drawable.ic_cactus_outline));
                NoteQuery query = new NoteQuery(this);
                query.updateFavorite(note.getName(), note.isFavorite(), v -> {});
                break;
            }
            case R.id.menu_note_view_delete: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.delete(note.getName(), v -> finish());
                break;
            }
            case R.id.menu_note_view_search:
                break;
            case R.id.menu_note_view_type:
                break;
            default: {
                @NoteType.Type int type;
                switch (item.getItemId()) {
                    case R.id.menu_note_view_type_markdown:
                        type = NoteType.TYPE_MARKDOWN;
                        typeItem.setIcon(R.drawable.ic_menu_md);
                        searchItem.setVisible(true);
                        break;
                    case R.id.menu_note_view_type_latex:
                        type = TYPE_MARKDOWN_LATEX;
                        typeItem.setIcon(R.drawable.ic_menu_latex);
                        searchItem.setVisible(false);
                        break;
                    default:
                        type = NoteType.TYPE_NORMAL;
                        typeItem.setIcon(R.drawable.ic_menu_note);
                        searchItem.setVisible(true);
                        break;
                }

                item.setChecked(true);
                if (type != note.getType()) {
                    note.setType(type);
                    setContent(text, type);
                    NoteQuery noteQuery = new NoteQuery(this);
                    noteQuery.updateType(note.getName(), type, v -> {});
                }
                setSearch();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK && data != null) {
            finish();
        } else if (requestCode == REQ_CATEGORY_EDIT && resultCode == RESULT_OK && data != null) {
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.updateCategory(note.getName(), new Category(data.getStringExtra("categoryName"), data.getIntExtra("categoryColor", -1)), v -> {});
            setCategory(data.getIntExtra("categoryColor", -1));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
