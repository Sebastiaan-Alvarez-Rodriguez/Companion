package com.python.companion.util.migration;

import androidx.annotation.NonNull;

public interface MigrationInterface {
    void onStatsAvailable(long categoryAmount, long notesAmount, long secureNotesAmount, long measurementAmount);

    void onStartCategories();
    void onCategoryProcessed();
    void onCategoryFailed();
    void onFinishCategories();

    void onStartNotes();
    void onNoteProcessed();
    void onNoteFailed();
    void onFinishNotes();

    void onStartMeasurements();
    void onMeasurementProcessed();
    void onMeasurementFailed();
    void onFinishMeasurements();

    /** Called when migration successfully finished */
    void onFinishMigration();
    /** Called when migration halts with a fatal error */
    void onFatalError(@NonNull String error);
}
