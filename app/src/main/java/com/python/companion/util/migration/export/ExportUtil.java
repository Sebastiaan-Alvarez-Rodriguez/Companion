package com.python.companion.util.migration.export;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.ConvertCallback;
import com.python.companion.security.converters.NoteConverter;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ExportUtil {
    public static final String header = "If the bytes don't align, then I will be not an export file\0\0\0\0\0";
    private static void exportCategory(@NonNull MessagePacker packer, @NonNull Category category) throws IOException {
        packer.packString(category.getCategoryName()).packInt(category.getCategoryColor());
    }

    @WorkerThread
    private static void exportCategories(@NonNull Context context, @NonNull MessagePacker packer, @Nullable ExportInterface exportInterface) {
        DAOCategory daoCategory = Database.getDatabase(context).getDAOCategory();
        List<Category> categories = daoCategory.getAll();
        try {
            packer.packInt(categories.size());
        } catch (IOException e) {
            Log.e("Export", "Problem packing category size", e);
            return;
        }

        int failed = 0, complete = 0;

        if (exportInterface != null)
            exportInterface.onStartExportCategories(categories.size());
        for (Category category : categories) {
            try {
                exportCategory(packer, category);
                ++complete;
            } catch (IOException e) {
                ++failed;
            }
            if (exportInterface != null)
                exportInterface.onCategoryProcessed(complete, failed, categories.size());
        }
    }

    private static void exportNote(@NonNull MessagePacker packer, @NonNull Note note) throws IOException {
        packer.packString(note.getName())
                .packString(note.getContent())
                .packString(note.getCategory().getCategoryName())
                .packInt(note.getCategory().getCategoryColor())
                .packInt(note.getType())
                .packLong(note.getModified().getEpochSecond());
    }

    private static void doExport(@NonNull List<Note> notes, MessagePacker packer, @Nullable ExportInterface exportInterface) {
        if (exportInterface != null)
            exportInterface.onStartExportNotes(notes.size());
        int failed = 0, complete = 0;
        for (Note note : notes) {
            try {
                exportNote(packer, note);
                ++complete;
            } catch (IOException e) {
                ++failed;
            }
            if (exportInterface != null)
                exportInterface.onNoteProcessed(complete, failed, notes.size());
        }
    }

    @WorkerThread
    private static void exportNotes(@NonNull FragmentManager manager, @NonNull Context context, @NonNull MessagePacker packer, boolean skipSecure, @Nullable ExportInterface exportInterface) {
        final List<Note> insecureNotes = Database.getDatabase(context).getDAONote().getInsecure();
        try {
            if (skipSecure) {
                packer.packInt(insecureNotes.size());
                packer.packInt(0);
                doExport(insecureNotes, packer, exportInterface);
            } else {
                final List<Note> secureNotes = Database.getDatabase(context).getDAONote().getSecure();
                int total = insecureNotes.size() + secureNotes.size();
                packer.packInt(total);
                packer.packInt(secureNotes.size());

                List<Note> decrypted = new ArrayList<>(total);
                if (exportInterface != null)
                    exportInterface.onStartDecryptNotes(secureNotes.size());

                NoteConverter.batchInsecure(manager, context, secureNotes, new ConvertCallback() {
                        @Override
                        public void onSuccess(@NonNull Note note) {
                            decrypted.add(note);
                            if (exportInterface != null)
                                exportInterface.onNoteDecryptProcessed(decrypted.size(), secureNotes.size());
                        }
                        @Override
                        public void onFailure() {}
                    });

                decrypted.addAll(insecureNotes);
                doExport(decrypted, packer, exportInterface);
                if (exportInterface != null)
                    exportInterface.onNoteDecryptFinished(decrypted.size());
            }
        } catch (IOException e) {
            Log.e("Export", "Big problem here ", e);
        }
    }

    public static void exportDatabase(@NonNull FragmentManager manager, @NonNull Context context, @NonNull Uri location, boolean skipSecure, @Nullable ExportInterface exportInterface) {
        ContentResolver contentResolver = context.getContentResolver();
        Executors.newSingleThreadExecutor().execute(() -> {
            try (MessagePacker packer = MessagePack.newDefaultPacker(contentResolver.openOutputStream(location))) {
                packer.packString(header);
                exportNotes(manager, context, packer, skipSecure, exportInterface);
                exportCategories(context, packer, exportInterface);

                if (exportInterface != null)
                    exportInterface.onExportComplete();
            } catch (IOException e) { //We get here if we cannot open file
                Log.e("Export", "exception with files? ", e);
            }
        });
    }
}
