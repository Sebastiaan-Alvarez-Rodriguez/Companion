package com.python.companion.ui.note.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;

public class NoteActivity extends AppCompatActivity {

    private EditText noteName, noteContent;
    private Button SaveButton;

    private boolean editMode;
    private String prevName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        findViews();
        setupClicks();
    }

    private void findViews() {
        noteName = findViewById(R.id.activity_note_name);
        noteContent = findViewById(R.id.activity_note_content);
        SaveButton = findViewById(R.id.activity_note_save);
    }

    private void setupClicks() {
        SaveButton.setOnClickListener(v -> save());
    }

    private void save() {
        if (noteName.getText().length() == 0)
            noteName.setError("This field must be filled");
        else if (editMode)
            checkEdit();
        else
            checkNew();
        //TODO: Create edit support
    }

    private void checkNew() {
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.isUnique(noteName.getText().toString(), unique -> {
            if (unique) {
                insertNew();
            } else {
                //TODO: show override dialog
            }
        });
    }

    private void checkEdit() {
        String currentName = noteName.getText().toString();
        boolean changedName = currentName.equals(prevName);
        if (!changedName) {
            updateExisting();
        } else {
            NoteQuery noteQuery = new NoteQuery(this);
            noteQuery.isUnique(currentName, unique -> {
                if (unique) {
                    updateExisting();
                } else {
                    //TODO: show override dialog
                }
            });
        }
    }

    private void insertNew() {
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.insert(noteName.getText().toString(), noteContent.getText().toString(), v -> finish());
    }

    private void updateExisting() {
        NoteQuery noteQuery = new NoteQuery(this);
        noteQuery.update(noteName.getText().toString(), noteContent.getText().toString(), v -> finish());
    }
}
