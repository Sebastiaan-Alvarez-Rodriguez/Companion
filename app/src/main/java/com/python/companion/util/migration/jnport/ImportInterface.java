package com.python.companion.util.migration.jnport;

public interface ImportInterface {
    void onStartImportNotes(int amount);
    void onNoteProcessed(int complete, int failed, int amount);

    void onStartEncryptNotes(int amount);
    void onNoteEncryptProcessed(int complete, int amount);

    void onStartImportCategories(int amount);
    void onCategoryProcessed(int complete, int failed, int amount);

    void onImportComplete();
}
