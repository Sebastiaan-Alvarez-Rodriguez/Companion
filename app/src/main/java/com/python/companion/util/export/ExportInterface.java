package com.python.companion.util.export;

public interface ExportInterface {
    void onStartDecryptNotes(int amount);
    void onNoteDecryptProcessed(int complete, int amount);
    void onNoteProcessed(int complete, int failed, int amount);

    void onStartExportCategories(int amount);
    void onCategoryProcessed(int complete, int failed, int amount);

}
