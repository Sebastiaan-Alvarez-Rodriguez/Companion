package com.python.companion.util.migration.export;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
            //TODO: Big problems here. Stop operations
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
    private static void exportNotes(@NonNull Context context, @NonNull MessagePacker packer, boolean skipSecure, @Nullable ExportInterface exportInterface) {
        final List<Note> notes = Database.getDatabase(context).getDAONote().getAll();
        final List<Note> secureNotes = notes.parallelStream().filter(Note::isSecure).collect(Collectors.toList());

        try {
            if (skipSecure) {
                int total = notes.size() - secureNotes.size();
                packer.packInt(total);
                packer.packInt(0);
                doExport(notes.parallelStream().filter(note -> !note.isSecure()).collect(Collectors.toList()), packer, exportInterface);
            } else {
                final List<Note> insecureNotes = notes.parallelStream().filter(note -> !note.isSecure()).collect(Collectors.toList());
                int total = notes.size();
                packer.packInt(total);
                packer.packInt(secureNotes.size());

                List<Note> decrypted = new ArrayList<>(total);

                if (exportInterface != null)
                    exportInterface.onStartDecryptNotes(secureNotes.size());

                for (Note note : secureNotes) {
                    //TODO below: Should display errordialog if problems occur
                    final int curSize = decrypted.size();
                    NoteConverter.makeNoteInsecure(context, note, exception -> {
                    }, insecureNote -> {
                        decrypted.add(insecureNote);
                        if (exportInterface != null)
                            exportInterface.onNoteDecryptProcessed(decrypted.size(), secureNotes.size());

                    });
                    while (curSize == decrypted.size())
                        Thread.sleep(1000);
                }

                decrypted.addAll(insecureNotes);
                doExport(decrypted, packer, exportInterface);
                if (exportInterface != null)
                    exportInterface.onNoteDecryptFinished(decrypted.size());
            }
        } catch (IOException e) {
            //TODO: Big problems here. Stop operations
            Log.e("Export", "Big problem here ", e);
        } catch (InterruptedException ignored) {}
    }

    public static void exportDatabase(@NonNull Context context, @NonNull Uri location, boolean skipSecure, @Nullable ExportInterface exportInterface) {
        ContentResolver contentResolver = context.getContentResolver();
        Executors.newSingleThreadExecutor().execute(() -> {
            try (MessagePacker packer = MessagePack.newDefaultPacker(contentResolver.openOutputStream(location))) {
                packer.packString(header);
                exportNotes(context, packer, skipSecure, exportInterface);
                exportCategories(context, packer, exportInterface);

                if (exportInterface != null)
                    exportInterface.onExportComplete();
            } catch (IOException e) { //We get here if we cannot open file
                Log.e("Export", "exception with files? ", e);
            }
        });
    }
}
