package com.python.companion.util.jnport;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessageInsufficientBufferException;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static com.python.companion.util.export.ExportUtil.header;

public class ImportUtil {
    private static Category importCategory(@NonNull MessageUnpacker unpacker) throws IOException {
        return new Category(unpacker.unpackString(), unpacker.unpackInt());
    }

    private static void importCategories(@NonNull Context context, @NonNull MessageUnpacker unpacker, @Nullable ImportInterface importInterface) {
        DAOCategory daoCategory = Database.getDatabase(context).getDAOCategory();
        try {
            if (!unpacker.hasNext() || unpacker.getNextFormat() != MessageFormat.POSFIXINT) {
                //TODO: Big problem here
                Log.e("Import", "I did not find an "+MessageFormat.POSFIXINT.toString()+", but got instead a "+  unpacker.getNextFormat().toString());
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
            daoCategory.upsert(categories.toArray(new Category[]{}));
        } catch (IOException e) {
            Log.e("ImportUtil", "Corrupt content", e);
        } catch (MessageInsufficientBufferException e) {
            Log.e("Import", "Corrupt content probably: ", e);
        }
    }

    private static Note importNote(@NonNull MessageUnpacker unpacker) throws IOException {
        //TODO: Allow option to make objects secure again (only those which were already secure
        Note tmp = new Note(unpacker.unpackString(), unpacker.unpackString(), new Category(unpacker.unpackString(), unpacker.unpackInt()), false, null, unpacker.unpackInt());
        tmp.setModified(Instant.ofEpochSecond(unpacker.unpackLong()));
        return tmp;
    }

    @WorkerThread
    private static void importNotes(@NonNull Context context, @NonNull MessageUnpacker unpacker, boolean reSecure, @Nullable ImportInterface importInterface) {
        DAONote daoNote = Database.getDatabase(context).getDAONote();

        try {
            if (!unpacker.hasNext() || unpacker.getNextFormat() != MessageFormat.POSFIXINT) {
                //TODO: Wrong content
                return;
            }
            int total = unpacker.unpackInt();
            List<Note> notes = new ArrayList<>(total);
            int prevSecure = unpacker.unpackInt();

            if (total < prevSecure) {
                //TODO: Wrong content
                return;
            }
            int complete = 0, failed = 0;


            if (importInterface != null)
                importInterface.onStartImportNotes(total);
            for (int pos = 0; pos < total; ++pos) {
                try {
                    Note tmp = importNote(unpacker);
                    notes.add(tmp);
                    Log.e("Import", "Found a note with name "+tmp.getName());
                    ++complete;
                } catch (IOException e) {
                    ++failed;
                }
                if (importInterface != null)
                    importInterface.onNoteProcessed(complete, failed, total);
            }

            if (reSecure) {
                if (importInterface != null)
                    importInterface.onStartEncryptNotes(prevSecure);

                for (int x = 0; x < prevSecure; ++x) {
                    Note tmp = notes.remove(0);
                    int curSize = notes.size();
                    NoteConverter.makeNoteSecure(context, tmp, notes::add);

                    while (curSize == notes.size())
                        Thread.sleep(1000);
                    if (importInterface != null)
                        importInterface.onNoteEncryptProcessed(x+1, prevSecure);
                }
            }

            daoNote.upsert(notes.toArray(new Note[]{}));

        } catch (IOException e) { // we only get here if we can somehow not unpack: Most likely can't open file
            Log.e("Import", "Cannot open file or something: ", e);
        } catch (MessageInsufficientBufferException e) {
            Log.e("Import", "Corrupt content probably: ", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkHeader(@NonNull MessageUnpacker unpacker) {
        try {
            if (!unpacker.hasNext())
                return false;
            return header.equals(unpacker.unpackString());
        } catch (IOException e) {
            return false;
        }
    }

    public static void importDatabase(@NonNull Context context, @NonNull Uri location, boolean reSecure, @Nullable ImportInterface importInterface) {
        Log.e("Import", "Import start!");
        ContentResolver contentResolver = context.getContentResolver();
        Executors.newSingleThreadExecutor().execute(() -> {
            try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(contentResolver.openInputStream(location))) {
                if (!checkHeader(unpacker)) {
                    //TODO: Header mismatch. Got no correct file.
                }

                importNotes(context, unpacker, reSecure, importInterface);
                importCategories(context, unpacker, importInterface);

                if (importInterface != null)
                    importInterface.onImportComplete();
                Log.e("Import", "Import finish!");
            } catch (IOException e) { //We get here if we cannot open file
                Log.e("Import", "Import file error? ", e);
            }
        });
    }
}
