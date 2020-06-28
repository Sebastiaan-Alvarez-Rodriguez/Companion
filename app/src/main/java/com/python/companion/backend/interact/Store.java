package com.python.companion.backend.interact;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.ConvertCallback;
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.ui.notes.note.dialog.LockDialog;
import com.python.companion.ui.notes.note.dialog.NoteOverrideDialog;

public class Store {
    /**
     * Insert a given new note into the database, either when there is no name-conflict, or the user tells us we may override
     * @param note Note to store. If its {@code secure} field is set, store the note securely
     * @param callback Callback receives a call in either {@link StoreCallback#onSuccess()) if we stored the new note, or {@link StoreCallback#onFailure()} if we did not
     */
    public static void insert(@NonNull Note note, @NonNull FragmentManager manager, @NonNull Context context, @NonNull StoreCallback callback) {
        NoteQuery noteQuery = new NoteQuery(context);
        noteQuery.isUniqueInstanced(note.getName(), result -> {
            if (result == null) { // Unique-named note
                insertInternal(note, note.isSecure(), manager, noteQuery, callback);
            } else { // Another note with same name exists
                showOverrideDialog(result, manager, new StoreCallback() {
                    @Override
                    public void onSuccess() {
                        insertInternal(note, note.isSecure(), manager, noteQuery, callback);
                    }
                    @Override
                    public void onFailure() {
                        callback.onFailure();
                    }
                });
            }
        });
    }

    /**
     * Update given note, either when there is no change in name, there is no name conflict, or when the user tells us we may override
     * @param note Note to update. If its {@code secure} field is set, store the note securely
     * @param prevName Name of note just before editing (needed to check name changes)
     * @param callback Callback receives a call in either {@link StoreCallback#onSuccess()) if we updated the note, or {@link StoreCallback#onFailure()} if we did not
     */
    public static void update(@NonNull Note note, String prevName, @NonNull FragmentManager manager, @NonNull Context context, @NonNull StoreCallback callback) {
        final NoteQuery noteQuery = new NoteQuery(context);
        if (!prevName.equals(note.getName())) {
            noteQuery.isUniqueInstanced(note.getName(), result -> {
                if (result == null) { // Unique-named note
                    updateInternal(note, note.isSecure(), manager, context, noteQuery, prevName, callback);
                } else { // Another note with same name exists
                    showOverrideDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            updateInternal(note, note.isSecure(), manager, context, noteQuery, prevName, callback);
                        }
                        @Override
                        public void onFailure() {
                            updateInternal(note, note.isSecure(), manager, context, noteQuery, prevName, callback);
                        }
                    });
                }
            });
        } else { //Name is the same as before
            updateInternal(note, note.isSecure(), manager, context, noteQuery, callback);
        }
    }

    /** Inserts given note, with specified security */
    private static void insertInternal(@NonNull Note note, boolean secure, @NonNull FragmentManager manager, @NonNull NoteQuery noteQuery, @NonNull StoreCallback callback) {
        if (secure) {
            LockDialog dialog = new LockDialog.Builder()
                    .setAcceptListener(n -> {
                        noteQuery.insert(n);
                        callback.onSuccess();
                    })
                    .setCancelListener(callback::onFailure)
                    .setNote(note)
                    .build(true);
            dialog.show(manager, null);
        } else {
            noteQuery.insert(note);
            callback.onSuccess();
        }
    }

    /** Equivalent to calling {@link Store#updateInternal(Note, boolean, FragmentManager, Context, NoteQuery, String, StoreCallback)} with {@code null} as replace candidate */
    private static void updateInternal(@NonNull Note note, boolean secure, @NonNull FragmentManager manager, @NonNull Context context, @NonNull NoteQuery noteQuery, @NonNull StoreCallback callback) {
        updateInternal(note, secure, manager, context, noteQuery, null, callback);
    }

    /**
     * Updates a given note. If specified, removes note with name {@code replaceCandidate} before inserting given note.
     * This is particularly useful when user updates the name of an existing note
     * @param note Note to store
     * @param secure Whether we want the note stored securely ({@code true}) or not ({@code false})
     * @param replaceCandidate Nullable candidate. Performs delete operation on note with given name if specified
     */
    private static void updateInternal(@NonNull Note note, boolean secure, @NonNull FragmentManager manager, @NonNull Context context, @NonNull NoteQuery noteQuery, @Nullable String replaceCandidate, @NonNull StoreCallback callback) {
        if (secure) {
            NoteConverter.makeNoteSecure(manager, context, note, new ConvertCallback() {
                @Override
                public void onSuccess(@NonNull Note n) {
                    if (replaceCandidate == null)
                        noteQuery.update(n, result -> callback.onSuccess());
                    else
                        noteQuery.replace(replaceCandidate, n, result -> callback.onSuccess());
                }
                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });
        } else {
            if (replaceCandidate == null)
                noteQuery.update(note, result -> callback.onSuccess());
            else
                noteQuery.replace(replaceCandidate, note, result -> callback.onSuccess());
        }
    }

    private static void showOverrideDialog(@NonNull Note conflicting, @NonNull FragmentManager manager, @NonNull StoreCallback callback) {
        NoteOverrideDialog noteOverrideDialog = new NoteOverrideDialog.Builder()
                .setExistsText("Note name already exists!")
                .setQuestionText("Do you want to override existing note?")
                .setWarningText("Warning: Overridden notes cannot be restored")
                .setNote(conflicting)
                .setOverrideListener(callback::onSuccess)
                .setCancelListener(callback::onFailure)
                .build();
        noteOverrideDialog.show(manager, null);
    }
}
