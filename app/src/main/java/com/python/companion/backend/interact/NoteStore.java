package com.python.companion.backend.interact;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.ui.notes.note.dialog.NoteOverrideDialog;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.FinishListener;

public class NoteStore {
    /**
     * Insert a given new note into the database, either when there is no name-conflict, or the user tells us we may override
     * @param note Note to store. If its {@code secure} field is set, store the note securely
     * @param finishListener Receives a call if we updated the note
     * @param errorListener Receives a call with a description of the error if we had an error while updating (incurs no changes in database)
     */
    public static void insert(@NonNull Note note, @NonNull FragmentManager manager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        NoteQuery noteQuery = new NoteQuery(context);
        noteQuery.isUniqueInstanced(note.getName(), result -> {
            if (result == null) { // Unique-named note
                insertInternal(note, note.isSecure(), manager, context, noteQuery, finishListener, errorListener);
            } else { // Another note with same name exists
                showOverrideDialog(result, manager, new StoreCallback() {
                    @Override
                    public void onSuccess() {
                        insertInternal(note, note.isSecure(), manager, context, noteQuery, finishListener, errorListener);
                    }
                    @Override
                    public void onFailure() {
                        errorListener.onError("Cancelled overriding");
                    }
                });
            }
        });
    }

    /**
     * Update given note, either when there is no change in name, there is no name conflict, or when the user tells us we may override
     * @param note Note to update. If its {@code secure} field is set, store the note securely
     * @param prevName Name of note just before editing (needed to check name changes)
     * @param finishListener Receives a call if we updated the note
     * @param errorListener Receives a call with a description of the error  if we had an error while updating (incurs no changes in database)
     */
    public static void update(@NonNull Note note, String prevName, @NonNull FragmentManager manager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        final NoteQuery noteQuery = new NoteQuery(context);
        if (!prevName.equals(note.getName())) {
            noteQuery.isUniqueInstanced(note.getName(), result -> {
                if (result == null) { // Unique-named note
                    updateInternal(note, note.isSecure(), manager, context, noteQuery, prevName, finishListener, errorListener);
                } else { // Another note with same name exists
                    showOverrideDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            updateInternal(note, note.isSecure(), manager, context, noteQuery, prevName, finishListener, errorListener);
                        }
                        @Override
                        public void onFailure() {
                            updateInternal(note, note.isSecure(), manager, context, noteQuery, prevName, finishListener, errorListener);
                        }
                    });
                }
            });
        } else { //Name is the same as before
            updateInternal(note, note.isSecure(), manager, context, noteQuery, finishListener, errorListener);
        }
    }

    /** Inserts given note, with specified security */
    private static void insertInternal(@NonNull Note note, boolean secure, @NonNull FragmentManager manager, @NonNull Context context, @NonNull NoteQuery noteQuery, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        if (secure) {
            NoteConverter.Encrypter.from(manager, context)
                    .setOnFinishListener(n -> {
                        noteQuery.insert(n);
                        finishListener.onFinish();
                    })
                    .setOnErrorListener(errorListener)
                    .encrypt(note);
        } else {
            noteQuery.insert(note);
            finishListener.onFinish();
        }
    }

    /** Equivalent to calling {@link Store#updateInternal(Note, boolean, FragmentManager, Context, NoteQuery, String, FinishListener, ErrorListener)} with {@code null} as replace candidate */
    private static void updateInternal(@NonNull Note note, boolean secure, @NonNull FragmentManager manager, @NonNull Context context, @NonNull NoteQuery noteQuery, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        updateInternal(note, secure, manager, context, noteQuery, null, finishListener, errorListener);
    }

    /**
     * Updates a given note. If specified, removes note with name {@code replaceCandidate} before inserting given note.
     * This is particularly useful when user updates the name of an existing note
     * @param note Note to store
     * @param secure Whether we want the note stored securely ({@code true}) or not ({@code false})
     * @param replaceCandidate Nullable candidate. Performs delete operation on note with given name if specified
     */
    private static void updateInternal(@NonNull Note note, boolean secure, @NonNull FragmentManager manager, @NonNull Context context, @NonNull NoteQuery noteQuery, @Nullable String replaceCandidate, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        if (secure) {
            NoteConverter.Encrypter.from(manager, context)
                    .setOnFinishListener(n -> {
                if (replaceCandidate == null)
                    noteQuery.update(n, result -> finishListener.onFinish());
                else
                    noteQuery.replace(replaceCandidate, n, result -> finishListener.onFinish());
            }).setOnErrorListener(errorListener)
                    .encrypt(note);
        } else {
            if (replaceCandidate == null)
                noteQuery.update(note, result -> finishListener.onFinish());
            else
                noteQuery.replace(replaceCandidate, note, result -> finishListener.onFinish());
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

    /** Callback to receive final state of store operations */
    public interface StoreCallback {
        void onSuccess();
        void onFailure();
    }
}
