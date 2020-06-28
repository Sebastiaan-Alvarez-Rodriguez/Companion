package com.python.companion.ui.notes.note.activity.view;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.ConvertCallback;
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.ui.notes.category.activity.CategoryEditActivity;
import com.python.companion.ui.notes.note.NoteContainer;
import com.python.companion.ui.notes.note.NoteType;
import com.python.companion.ui.notes.note.activity.edit.NoteEditActivity;
import com.python.companion.util.ColorUtil;
import com.python.companion.util.RenderUtil;

import static com.python.companion.ui.notes.note.NoteType.TYPE_MARKDOWN_LATEX;

public class NoteViewActivity extends AppCompatActivity {
    private static final int REQ_EDIT = 1;
    private static final int REQ_CATEGORY_EDIT = 2;
    private TextView contentView;
    private FloatingActionButton editButton;

    private Note note;
    private String text;

    private MenuItem lockItem, categoryItem, typeItem;

    private NoteViewViewModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        model = new ViewModelProvider(this).get(NoteViewViewModel.class);

        Intent intent = getIntent();
        note = ((NoteContainer) intent.getParcelableExtra("note")).getNote();
        text = note.isSecure() ? intent.getStringExtra("plaintext") : note.getContent();

        Log.e("View", "Note type received: "+note.getType());
        findViews();
        setupButton();
        setupActionBar();
        setContent(text, note.getType());
    }

    private void findViews() {
        contentView = findViewById(R.id.activity_note_view_content);
        editButton = findViewById(R.id.activity_note_view_edit);
    }

    private void setupButton() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.putExtra("note", new NoteContainer(new Note(note.getName(), text, note.getCategory(), note.isSecure(), note.getIv(), note.getType())));
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
            Drawable icon = myToolbar.getNavigationIcon();
            if (icon != null) {
                icon.setColorFilter(new BlendModeColorFilter(getResources().getColor(R.color.colorWindowBackground, null), BlendMode.SRC_IN));
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle(note.getName());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_view, menu);
        lockItem = menu.findItem(R.id.menu_note_view_lock);
        categoryItem = menu.findItem(R.id.menu_note_view_edit_category);
        typeItem = menu.findItem(R.id.menu_note_view_type);

        model.getNote(note.getName()).observe(this, n -> { //Observes note to automatically fetch category
            if (!note.getCategory().equals(n.getCategory())) {
                int categoryColor = n.getCategory().getCategoryColor();
                double diff = ColorUtil.computeDiff(categoryColor, getColor(R.color.colorPrimary));
                if (Math.abs(diff) < 4.0)
                    categoryColor = getColor(R.color.colorWindowBackground);
                categoryItem.getIcon().setColorFilter(new BlendModeColorFilter(categoryColor, BlendMode.SRC_IN));
            }
        });
        lockItem.setIcon(getDrawable(note.isSecure() ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline));
        switch (note.getType()) {
            case NoteType.TYPE_MARKDOWN:
                menu.findItem(R.id.menu_note_view_type_markdown).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_md);
                break;
            case TYPE_MARKDOWN_LATEX:
                menu.findItem(R.id.menu_note_view_type_latex).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_latex);
                break;
            case NoteType.TYPE_NORMAL:
            default:
                menu.findItem(R.id.menu_note_view_type_normal).setChecked(true);
                typeItem.setIcon(R.drawable.ic_menu_note);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_note_view_lock: {
                ConvertCallback callback = new ConvertCallback() {
                    @Override
                    public void onSuccess(@NonNull Note n) {
                        NoteQuery query = new NoteQuery(getApplicationContext());
                        query.update(n, v -> {});
                        note = n;
                        lockItem.setIcon(getDrawable(note.isSecure() ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline));
                        Snackbar.make(contentView, "Successfully changed lock status!", Snackbar.LENGTH_LONG).show();
                    }
                    @Override
                    public void onFailure() {
                        Snackbar.make(contentView, "Failure: Did not change lock status!", Snackbar.LENGTH_LONG).show();
                    }
                };
                if (note.isSecure())
                    NoteConverter.makeNoteInsecure(getSupportFragmentManager(), getApplicationContext(), note, callback);
                else
                    NoteConverter.makeNoteSecure(getSupportFragmentManager(), getApplicationContext(), note, callback);
                break;
            }
            case R.id.menu_note_view_edit_category: {
                Intent intent = new Intent(this, CategoryEditActivity.class);
                intent.putExtra("categoryName", note.getCategory().getCategoryName());
                intent.putExtra("categoryColor", note.getCategory().getCategoryColor());
                startActivityForResult(intent, REQ_CATEGORY_EDIT);
                break;
            }
            case R.id.menu_note_view_delete: {
                NoteQuery noteQuery = new NoteQuery(this);
                noteQuery.delete(note.getName(), v -> finish());
                break;
            }
            case R.id.menu_note_view_type:
                break;
            default: {
                @NoteType.Type int type;
                switch (item.getItemId()) {
                    case R.id.menu_note_view_type_markdown:
                        type = NoteType.TYPE_MARKDOWN;
                        typeItem.setIcon(R.drawable.ic_menu_md);
                        break;
                    case R.id.menu_note_view_type_latex:
                        type = TYPE_MARKDOWN_LATEX;
                        typeItem.setIcon(R.drawable.ic_menu_latex);
                        break;
                    default:
                        type = NoteType.TYPE_NORMAL;
                        typeItem.setIcon(R.drawable.ic_menu_note);
                        break;
                }

                item.setChecked(true);
                Log.e("View", "Clicked on note ("+note.getName()+") type change item. Normal: "+(type==NoteType.TYPE_NORMAL)+". MD: "+(type==NoteType.TYPE_MARKDOWN)+". Latex: "+(type== TYPE_MARKDOWN_LATEX));
                if (type != note.getType()) {
                    Log.e("View", "This is another type than before");
                    note.setType(type);
                    setContent(text, type);
                    NoteQuery noteQuery = new NoteQuery(this);
                    noteQuery.updateType(note.getName(), type, v -> {
                        Log.e("ViewCB", "Note updated. New type: "+note.getType());
                    });
                }
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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
