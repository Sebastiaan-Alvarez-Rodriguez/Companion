package com.python.companion.util.jnport;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.db.constant.CategoryQuery;
import com.python.companion.db.constant.NoteQuery;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportUtil {
    private static Category importCategory(@NonNull MessageUnpacker unpacker) throws IOException {
        return new Category(unpacker.unpackString(), unpacker.unpackInt());
    }

    private static void importCategories(@NonNull Context context, @NonNull MessageUnpacker unpacker, @Nullable ImportInterface importInterface) {
        CategoryQuery categoryQuery = new CategoryQuery(context);
        try {
            if (!unpacker.hasNext() || unpacker.getNextFormat() != MessageFormat.INT32) {
                //TODO: Big problem here
                return;
            }
            int total = unpacker.unpackInt();
            List<Category> categories = new ArrayList<>(total);
            int complete = 0, failed = 0;

            if (importInterface != null)
                importInterface.onStartImportCategories(total);
            for (int pos = 0; pos < total; ++pos) {
                try {
                    categories.add(importCategory(unpacker));
                    ++complete;
                } catch (IOException e) {
                    ++failed;
                }
                if (importInterface != null)
                    importInterface.onCategoryProcessed(complete, failed, total);
            }
            categoryQuery.insert(categories.toArray(new Category[]{}));
        } catch (IOException e) {
            Log.e("ImportUtil", "Corrupt content", e);
        } catch (MessageInsufficientBufferException e) {
            Log.e("Import", "Corrupt content probably: ", e);
        }
    }

    private static Note importNote(@NonNull MessageUnpacker unpacker) throws IOException {
        //TODO: Allow option to make objects secure again (only those which were already secure
        return new Note(unpacker.unpackString(), unpacker.unpackString(), new Category(unpacker.unpackString(), unpacker.unpackInt()), false, null, unpacker.unpackInt());
    }

    private static void importNotes(@NonNull Context context, @NonNull MessageUnpacker unpacker, boolean reSecure, @Nullable ImportInterface importInterface) {
        NoteQuery noteQuery = new NoteQuery(context);
        try {
            if (!unpacker.hasNext() || unpacker.getNextFormat() != MessageFormat.INT32) {
                //TODO: Big problem here
                return;
            }
            int total = unpacker.unpackInt();
            List<Note> notes = new ArrayList<>(total);
            int prevSecure = unpacker.unpackInt();

            int secureFailed = 0;

            int pos = 0;

            if (reSecure) {
                if (importInterface != null)
                    importInterface.onStartEncryptNotes(prevSecure);

                for (; pos < prevSecure; ++pos) {
                    try {
                        Note tmp = importNote(unpacker);
                        NoteConverter.makeNoteSecure(context, tmp, secureNote -> {
                            notes.add(tmp);
                            if (importInterface != null)
                                importInterface.onNoteEncryptProcessed(notes.size(), prevSecure);
                        });
                    } catch (IOException e) {
                        ++secureFailed;
                    }
                }
            }
            int complete = reSecure ? prevSecure - secureFailed : 0, failed = 0;

            if (importInterface != null)
                importInterface.onStartImportNotes(complete, total);
            for (; pos < total; ++pos) {
                try {
                    notes.add(importNote(unpacker));
                    ++complete;
                } catch (IOException e) {
                    ++failed;
                }
                if (importInterface != null)
                    importInterface.onNoteProcessed(complete, failed, total);
            }

            noteQuery.insert(notes.toArray(new Note[]{}));

            if (importInterface != null)
                importInterface.onImportComplete(complete, failed, total);
        } catch (IOException e) { // we only get here if we can somehow not unpack: Most likely can't open file
            Log.e("Import", "Cannot open file or something: ", e);
        } catch (MessageInsufficientBufferException e) {
            Log.e("Import", "Corrupt content probably: ", e);
        }
    }

    public static void importDatabase(@NonNull Context context, @NonNull Uri location, boolean reSecure, @Nullable ImportInterface importInterface) {
        ContentResolver contentResolver = context.getContentResolver();
        try (InputStream inStream = contentResolver.openInputStream(location)){
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(inStream);
            importNotes(context, unpacker, reSecure, importInterface);
            importCategories(context, unpacker, importInterface);
        } catch (IOException e) { //We get here if we cannot open file

        }
    }
}
