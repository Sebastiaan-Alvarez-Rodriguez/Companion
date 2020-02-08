package com.python.companion.ui.note.activity.edit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.python.companion.R;
import com.python.companion.db.constant.NoteQuery;

public class NoteEditActivity extends AppCompatActivity {
    private static final int REQUEST_PREVIEW = 1;
    private EditText noteName, noteContent;
    private Button previewButton;

    private boolean editMode;
    private String prevName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);
        findViews();

        Intent inputIntent = getIntent();
        editMode = inputIntent.hasExtra("name");
        if (editMode) {
            prevName = inputIntent.getStringExtra("name");
            noteName.setText(prevName);
            NoteQuery noteQuery = new NoteQuery(this);
            noteContent.setEnabled(false);
            noteQuery.getContent(prevName, content -> {
                noteContent.setText(content);
                noteContent.setEnabled(true);
            });
        }
        setupClicks();
    }

    private void findViews() {
        noteName = findViewById(R.id.activity_note_edit_name);
        noteContent = findViewById(R.id.activity_note_edit_content);
        previewButton = findViewById(R.id.activity_note_edit_preview);
    }

    private void setupClicks() {
        previewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotePreviewActivity.class);
            intent.putExtra("name", noteName.getText().toString());
            intent.putExtra("content", noteContent.getText().toString());
            if (editMode)
                intent.putExtra("prevName", prevName);
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
        Log.i("Edit", "Received resultcode "+resultCode+" for request "+requestCode);
    }
}
