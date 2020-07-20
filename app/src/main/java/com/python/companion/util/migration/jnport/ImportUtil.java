package com.python.companion.util.migration.jnport;

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

import static com.python.companion.util.migration.export.ExportUtil.header;

public class ImportUtil {
    private static Category importCategory(@NonNull MessageUnpacker unpacker) throws IOException {
        return new Category(unpacker.unpackString(), unpacker.unpackInt());
    }

    private static void importCategories(@NonNull Context context, @NonNull MessageUnpacker unpacker, @Nullable ImportInterface importInterface) {
        DAOCategory daoCategory = Database.getDatabase(context).getDAOCategory();
        try {
            if (!unpacker.hasNext() || unpacker.getNextFormat() != MessageFormat.POSFIXINT) {
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
        String name = unpacker.unpackString();
        String content = unpacker.unpackString();
        String catname = unpacker.unpackString();
        int catcolor = unpacker.unpackInt();
        Instant i = Instant.ofEpochSecond(unpacker.unpackLong());
        int type = unpacker.unpackInt();
        boolean fav = unpacker.unpackBoolean();
        return new Note(name, content, new Category(catname, catcolor), false, null, i, type, fav);
    }

    @WorkerThread
    private static void importNotes(@NonNull FragmentManager manager, @NonNull Context context, @NonNull MessageUnpacker unpacker, boolean reSecure, @Nullable ImportInterface importInterface) {
        DAONote daoNote = Database.getDatabase(context).getDAONote();

        try {
            if (!unpacker.hasNext() || unpacker.getNextFormat() != MessageFormat.POSFIXINT) {
                if (!unpacker.hasNext())
                    Log.e("Import", "No more content! Empty file probably");
                else
                    Log.e("Import", "No postfixint?");
                return;
            }
            int total = unpacker.unpackInt();
            List<Note> notes = new ArrayList<>(total);
            int prevSecure = unpacker.unpackInt();

            if (total < prevSecure) {
                Log.e("Import","File states there are "+total+" notes in total, of which "+prevSecure+" were secure");
                return;
            }
            int complete = 0, failed = 0;


            if (importInterface != null)
                importInterface.onStartImportNotes(total);
            for (int x = 0; x < prevSecure; ++x) {
                try {
                    notes.add(importNote(unpacker));
                    ++complete;
                } catch (IOException e) {
                    ++failed;
                }
            }
            if (reSecure && prevSecure > 0) {
                if (importInterface != null)
                    importInterface.onStartEncryptNotes(prevSecure);
                List<Integer> tmp = new ArrayList<>(1);
                tmp.add(0);
                NoteConverter.batchEncrypt(manager, context, notes, new NoteConverter.ConvertCallback() {
                    @Override
                    public void onSuccess(@NonNull Note note) {
                        notes.set(tmp.get(0), note);
                        if (importInterface != null)
                            importInterface.onNoteEncryptProcessed(tmp.get(0)+1, prevSecure);
                        tmp.set(0, tmp.get(0)+1);
                    }
                    @Override
                    public void onFailure() {}
                });
            }
            for (int x = 0; x < total-prevSecure; ++x) {
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
            daoNote.upsert(notes.toArray(new Note[]{}));
        } catch (IOException e) { // we only get here if we can somehow not unpack: Most likely can't open file
            Log.e("Import", "Cannot open file or something: ", e);
        } catch (MessageInsufficientBufferException e) {
            Log.e("Import", "Corrupt content probably: ", e);
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

    public static void importDatabase(@NonNull FragmentManager manager, @NonNull Context context, @NonNull Uri location, boolean reSecure, @Nullable ImportInterface importInterface) {
        Log.e("Import", "Import start!");
        ContentResolver contentResolver = context.getContentResolver();
        Executors.newSingleThreadExecutor().execute(() -> {
            try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(contentResolver.openInputStream(location))) {
                if (!checkHeader(unpacker)) {
                    Log.e("Import", "Got a header mismatch!");
                    return;
                }

                importNotes(manager, context, unpacker, reSecure, importInterface);
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
