package com.python.companion.ui.notes.note.activity.view;

import android.content.Intent;
import android.graphics.PorterDuff;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.notes.category.activity.CategoryEditActivity;
import com.python.companion.ui.notes.note.NoteType;
import com.python.companion.ui.notes.note.activity.edit.NoteEditActivity;
import com.python.companion.ui.notes.note.dialog.LockDialog;
import com.python.companion.util.ColorUtil;
import com.python.companion.util.RenderUtil;

public class NoteViewActivity extends AppCompatActivity {
    private static final int REQ_EDIT = 1;
    private static final int REQ_CATEGORY_EDIT = 2;
    private TextView contentView;
    private FloatingActionButton editButton;

    private String curName;
    private String curContent;
    private Category curCategory;
    private @NoteType.Type int curType;
    private boolean curSecure;

    private MenuItem lockItem, categoryItem, typeItem;
    private MenuItem normalItem, mdItem, latexItem;

    private NoteViewViewModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_view);
        model = new ViewModelProvider(this).get(NoteViewViewModel.class);

        Intent intent = getIntent();
        curName = intent.getStringExtra("name");
        curContent = intent.getStringExtra("content");
        curCategory = null;
        curType = -1;
        curSecure = true;
        findViews();
        setupButton();
        setupActionBar();
    }

    private void findViews() {
        contentView = findViewById(R.id.activity_note_view_content);
        editButton = findViewById(R.id.activity_note_view_edit);
    }

    private void setupButton() {
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditActivity.class);
            intent.putExtra("name", curName);
            intent.putExtra("content", curContent);
            intent.putExtra("categoryName", curCategory.getCategoryName());
            intent.putExtra("categoryColor", curCategory.getCategoryColor());
            intent.putExtra("secure", curSecure);
            intent.putExtra("type", curType);
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
                icon.setColorFilter(getResources().getColor(R.color.colorWindowBackground, null), PorterDuff.Mode.SRC_IN);
                myToolbar.setNavigationIcon(icon);
            }
            actionbar.setTitle(curName);
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
                noteQuery.get(curName, note -> {
                    LockDialog dialog = new LockDialog.Builder()
                            .setAcceptListener(noteSecured -> {
                                noteQuery.update(noteSecured, v -> {});
                                Snackbar.make(contentView, "Successfully changed lock status!", Snackbar.LENGTH_LONG).show();
                            })
                            .setNote(note)
                            .build();
                    runOnUiThread(() -> dialog.show(getSupportFragmentManager(), null));
                });
                break;
            }
            case R.id.menu_note_view_edit_category: {
                Intent intent = new Intent(this, CategoryEditActivity.class);
                if (curCategory != null) {
                    intent.putExtra("categoryName", curCategory.getCategoryName());
                    intent.putExtra("categoryColor", curCategory.getCategoryColor());
                }
                startActivityForResult(intent, REQ_CATEGORY_EDIT);
                break;
            }
            case R.id.menu_note_view_delete: {
                    NoteQuery noteQuery = new NoteQuery(this);
                    noteQuery.delete(curName, v -> finish());
                    break;
                }
            case R.id.menu_note_view_type:
                break;
            default: {
                NoteQuery noteQuery = new NoteQuery(this);
                @NoteType.Type int type;
                switch (item.getItemId()) {
                    case R.id.menu_note_view_type_markdown:
                        type = NoteType.TYPE_MARKDOWN;
                        break;
                    case R.id.menu_note_view_type_latex:
                        type = NoteType.TYPE_MARKDOWN_LATEX;
                        break;
                    default:
                        type = NoteType.TYPE_NORMAL;
                        break;
                }
                noteQuery.updateType(curName, type, v -> {});
                item.setChecked(true); //TODO: Redundant call?
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_note_view, menu);
        lockItem = menu.findItem(R.id.menu_note_view_lock);
        categoryItem = menu.findItem(R.id.menu_note_view_edit_category);
        normalItem = menu.findItem(R.id.menu_note_view_type_normal);
        mdItem = menu.findItem(R.id.menu_note_view_type_markdown);
        latexItem = menu.findItem(R.id.menu_note_view_type_latex);
        typeItem = menu.findItem(R.id.menu_note_view_type);

        model.getNote(curName).observe(this, note -> update(note.getName(), curContent, note.getCategory(), note.getType(), note.isSecure()));
        return true;
    }

    private void update(@NonNull String name, @NonNull String content, @NonNull Category category, @NoteType.Type int type, boolean secure) {
        curName = name;
        if (!content.equals(curContent)) {
            curContent = content;
            if (type == curType)
                setContent(curContent, curType);
        }
        if (category != curCategory) {
            int categoryColor = category.getCategoryName().length() == 0 ? getColor(R.color.colorPrimary) : category.getCategoryColor();
            double diff = ColorUtil.computeDiff(categoryColor, getColor(R.color.colorPrimary));
            if (Math.abs(diff) < 4.0)
                categoryColor = getColor(R.color.colorWindowBackground);

            categoryItem.getIcon().setColorFilter(categoryColor, PorterDuff.Mode.SRC_IN);
            curCategory = category;
        }

        if (type != curType) {
            setContent(curContent, type);
            curType = type;
            Drawable d;
            switch (type) {
                case NoteType.TYPE_MARKDOWN:
                    d = getDrawable(R.drawable.ic_menu_md);
                    mdItem.setChecked(true);
                    break;
                case NoteType.TYPE_MARKDOWN_LATEX:
                    d = getDrawable(R.drawable.ic_menu_latex);
                    latexItem.setChecked(true);
                    break;
                case NoteType.TYPE_NORMAL: default:
                    d = getDrawable(R.drawable.ic_menu_note);
                    normalItem.setChecked(true);
                    break;
            }
            typeItem.setIcon(d);
        }

        if (secure != curSecure) {
            lockItem.setIcon(getDrawable(secure ? R.drawable.ic_lock_outline : R.drawable.ic_lock_open_outline));
            curSecure = secure;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK && data != null) {
            update(curName, data.getStringExtra("content"), curCategory, curType, curSecure);
        } else if (requestCode == REQ_CATEGORY_EDIT && resultCode == RESULT_OK && data != null) {
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.updateCategory(curName, new Category(data.getStringExtra("categoryName"), data.getIntExtra("categoryColor", -1)), v -> {});
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
