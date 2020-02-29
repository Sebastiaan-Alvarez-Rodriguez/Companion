package com.python.companion.util.export;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExportUtil {
    private static void exportCategory(@NonNull MessagePacker packer, @NonNull Category category) throws IOException {
        packer.packString(category.getCategoryName()).packInt(category.getCategoryColor());
    }

    private static void exportCategories(@NonNull Context context, @NonNull MessagePacker packer, @Nullable ExportInterface exportInterface) {
        CategoryQuery categoryQuery = new CategoryQuery(context);
        categoryQuery.getAll(categories -> {
            try {
                packer.packInt(categories.size());
            } catch (IOException e) {
                //TODO: Big problems here. Stop operations
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
        });
    }

    private static void exportNote(@NonNull MessagePacker packer, @NonNull Note note) throws IOException {
        packer.packString(note.getName())
                .packString(note.getContent())
                .packString(note.getCategory().getCategoryName())
                .packInt(note.getCategory().getCategoryColor())
                .packInt(note.getType())
                .packLong(note.getModified().getEpochSecond());
    }

    private static void exportNotes(@NonNull Context context, @NonNull MessagePacker packer, @Nullable ExportInterface exportInterface) {
        NoteQuery noteQuery = new NoteQuery(context);
        noteQuery.getAll(notes -> {
            List<Note> secureNotes = notes.parallelStream().filter(Note::isSecure).collect(Collectors.toList());
            if (exportInterface != null)
                exportInterface.onStartDecryptNotes(secureNotes.size());

            try {
                packer.packInt(notes.size());
                packer.packInt(secureNotes.size());
            } catch (IOException e) {
                //TODO: Big problems here. Stop operations
                return;
            }

            List<Note> insecureNotes = new ArrayList<>(notes.size());
            for (Note note : secureNotes) {
                //TODO below: Should display errordialog if problems occur
                NoteConverter.makeNoteInsecure(context, note, exception -> {}, insecureNote -> {
                    insecureNotes.add(insecureNote);
                    if (exportInterface != null)
                        exportInterface.onNoteDecryptProcessed(insecureNotes.size(), secureNotes.size());
                });
            }

            insecureNotes.addAll(notes.parallelStream().sorted((o1, o2) -> (o1.isSecure() == o2.isSecure()) ? 0 : (o1.isSecure() ? -1 : 1)).collect(Collectors.toList()));

            int nFailed = 0, nComplete = 0;
            for (Note note : insecureNotes) {
                try {
                    exportNote(packer, note);
                    ++nComplete;
                } catch (IOException e) {
                    ++nFailed;
                }
                if (exportInterface != null)
                    exportInterface.onNoteProcessed(nComplete, nFailed, insecureNotes.size());
            }

            exportCategories(context, packer, exportInterface);
        });
    }

    public static void exportDatabase(@NonNull Context context, @NonNull Uri location, @Nullable ExportInterface exportInterface) {
        ContentResolver contentResolver = context.getContentResolver();
        try (OutputStream outStream = contentResolver.openOutputStream(location)) {
            MessagePacker packer = MessagePack.newDefaultPacker(outStream);
            exportNotes(context, packer, exportInterface);
            packer.close();
        } catch (IOException e) { //We get here if we cannot open file

        }
    }
}
