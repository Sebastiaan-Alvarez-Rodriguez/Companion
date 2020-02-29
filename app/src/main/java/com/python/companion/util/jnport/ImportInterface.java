package com.python.companion.util.jnport;

public interface ImportInterface {
    void onStartEncryptNotes(int amount);
    void onNoteEncryptProcessed(int complete, int amount);
    void onNoteProcessed(int complete, int failed, int amount);

    void onStartImportCategories(int amount);
    void onCategoryProcessed(int complete, int failed, int amount);
}
