package com.python.companion.ui.notes.note.activity.edit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.python.companion.R;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.notes.note.NoteContainer;
import com.python.companion.ui.notes.note.activity.view.NotePreviewActivity;

/**
 * Redirect users here to create a new note or edit an existing one. If the user wants to edit a note,
 * just add the note under key {@code "note"} in the intent, with the note shipped in a NoteContainer (which is parcelable)
 * @see NoteContainer
 */
public class NoteEditActivity extends AppCompatActivity {
    private static final int REQUEST_PREVIEW = 1;
    private EditText noteName, noteContent;
    private Button previewButton;

    private @Nullable Note note;
    private boolean editMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);
        findViews();

        Intent intent = getIntent();
        editMode = intent.hasExtra("note");
        if (editMode) {
            note = ((NoteContainer) intent.getParcelableExtra("note")).getNote();
            noteName.setText(note.getName());
            noteContent.setText(note.getContent());
        }
        setupClicks();
    }

    private void findViews() {
        noteName = findViewById(R.id.activity_note_edit_name);
        noteContent = findViewById(R.id.activity_note_edit_content);
        previewButton = findViewById(R.id.activity_note_edit_preview);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupClicks() {
        previewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotePreviewActivity.class);
            if (editMode) {
                Note n = new Note(noteName.getText().toString(), noteContent.getText().toString(), note.getCategory(), note.isSecure(), note.getIv(), note.getType(), note.isFavorite());
                intent.putExtra("note", new NoteContainer(n));
                intent.putExtra("prevName", note.getName());
            } else {
                intent.putExtra("note", new NoteContainer(new Note(noteName.getText().toString(), noteContent.getText().toString())));
            }
            startActivityForResult(intent, REQUEST_PREVIEW);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PREVIEW && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (editMode && noteName.getText().toString().equals(note.getName()) && noteContent.getText().toString().equals(note.getContent()))
            super.onBackPressed();
        else if (noteName.getText().length() == 0 && noteContent.getText().length() == 0)
            super.onBackPressed();
        else
            new AlertDialog.Builder(this)
                    .setMessage("Go back without saving?")
                    .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton("Go back", (dialog12, which) -> finish())
                    .setOnCancelListener(dialog -> finish())
                    .show();
    }
}
