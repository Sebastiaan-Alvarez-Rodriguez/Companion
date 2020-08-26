package com.python.companion.util.migration;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOCategory;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.security.converters.NoteConverter;
import com.python.companion.util.ThreadUtil;
import com.python.companion.util.genericinterfaces.ResultListener;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Class used to import an exported database backup, and load all data back into the database
 *
 * THE JNPORT/EXPORT PROTOCOL:
 * 128-bit (16 byte) header, used to check whether user actually picked a database exported from this app
 * long 32-bit #categories
 * long 32-bit #notes
 * long 32-bit #secure notes (cannot be higher than #notes)
 * long 32-bit #anniversaries
 * DATA-1: All categories
 * DATA-2.1: All non-secure notes
 * DATA-2.2: All secure notes
 * DATA-3: All anniversaries
 * Check <code>visit(Type type)</code> functions to find how categories, notes, anniversaries must be stored
 */
public class Importer implements EntityVisitor {
    protected MessageUnpacker unpacker;

    protected @NonNull FragmentManager manager;
    protected @NonNull Context context;

    protected @Nullable MigrationInterface migrationInterface;

    public static Importer from(@NonNull FragmentManager manager, @NonNull Context context) {
        return new Importer(manager, context);
    }

    protected Importer(@NonNull FragmentManager manager, @NonNull Context context) {
        this.manager = manager;
        this.context = context;
    }

    public Importer with(@Nullable MigrationInterface migrationInterface) {
        this.migrationInterface = migrationInterface;
        return this;
    }
    /**
     * Visit a single Category.
     * Category fields are fetched in order of declaration.
     * @param category Default category whose values are overridden with the values we read
     */
    @Override
    public void visit(@NonNull Category category) {
        try {
            category.setCategoryName(unpacker.unpackString());
            category.setCategoryColor(unpacker.unpackInt());
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onCategoryProcessed);
        } catch (IOException e) {
            Log.e("Importer", "Big big error (category): ", e);
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onCategoryFailed);
        }
    }

    /**
     * Visit a single Note.
     * Note fields are fetched in order of declaration.
     * A few fields are skipped: <var>iv</var> and <var>secure</var>}
     * This is because we know all data is not secure (stored plaintext in backup file),
     * so there can be no <var>iv</var> and <var>secure</var> is false
     * @param note Default note whose values are overridden with the values we read
     */
    @Override
    public void visit(@NonNull Note note) {
        try {
            String name = unpacker.unpackString();
            String content = unpacker.unpackString();
            String catname = unpacker.unpackString();
            int catcolor = unpacker.unpackInt();
            Instant i = Instant.ofEpochSecond(unpacker.unpackLong());
            int type = unpacker.unpackInt();
            boolean fav = unpacker.unpackBoolean();

            note.setName(name);
            note.setContent(content);
            note.setCategory(new Category(catname, catcolor));
            note.setModified(i);
            note.setType(type);
            note.setFavorite(fav);

            note.setSecure(false);

            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onNoteProcessed);
        } catch (IOException e) {
            Log.e("Importer", "Big big error (note): ", e);
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onNoteFailed);
        }
    }

    /**
     * Visit a single Anniversary.
     * Anniversary fields are fetched in order of declaration.
     * @param anniversary Default anniversary whose values are overridden with the values we read
     */
    @Override
    public void visit(@NonNull Anniversary anniversary) {
        try {
            long id = unpacker.unpackLong();
            String singular = unpacker.unpackString();
            String plural = unpacker.unpackString();
            Duration duration = Duration.parse(unpacker.unpackString());
            long amount = unpacker.unpackLong();
            long precomputedamount = unpacker.unpackLong();
            long parentID = unpacker.unpackLong();
            ChronoUnit cornerstoneType = ChronoUnit.valueOf(unpacker.unpackString());
//            boolean hasNotifications = unpacker.unpackBoolean(); TODO: Uncomment this line when ready

            anniversary.setAnniversaryID(id);
            anniversary.setNameSingular(singular);
            anniversary.setNamePlural(plural);
            anniversary.setDuration(duration);
            anniversary.setAmount(amount);
            anniversary.setPrecomputedamount(precomputedamount);
            anniversary.setParentID(parentID);
            anniversary.setCornerstoneType(cornerstoneType);
//            anniversary.setHasNotifications(hasNotifications); TODO: Uncomment this line when ready
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onAnniversaryProcessed);
        } catch (IOException e) {
            Log.e("Importer", "Big big error (anniversary): ", e);
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onAnniversaryFailed);
        }
    }

    protected void jnportCategories(long amount) {
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onStartCategories);

        Category[] categoryList = new Category[(int) amount];
        for (int x = 0; x < amount; ++x) {
            Category c = new Category("", 0);
            visit(c);
            categoryList[x] = c;
        }
        DAOCategory daoCategory = Database.getDatabase(context).getDAOCategory();
        daoCategory.upsert(categoryList);

        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onFinishCategories);
    }

    private void jnportNotesInternal(long amount, @NonNull ResultListener<ArrayList<Note>> resultListener) {
        Runtime runtime = Runtime.getRuntime();
        ArrayList<Note> noteList = new ArrayList<>((int) amount);
        for (int x = 0; x < amount; ++x) {
            Note n = Note.template();
            visit(n);

            noteList.add(n);
            if (runtime.freeMemory() < 100*1024*1024) { // We have less than 100MB available. Next note might be 100MB (although unlikely). Clean cache
                resultListener.onResult(noteList);
                noteList = new ArrayList<>((int) amount);
            }
        }
        if (!noteList.isEmpty()) {
            resultListener.onResult(noteList);
        }
    }

    protected boolean jnportNotes(@NonNull FragmentManager manager, long amount, long amountSecure, boolean reSecure) {
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onStartNotes);

        if (amount < amountSecure) {
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(() -> migrationInterface.onFatalError("Corrupted file: More secure notes than notes"));
            return false;
        }

        final DAONote daoNote = Database.getDatabase(context).getDAONote();

        reSecure &= amountSecure > 0;
        long amountNonSecure = amount - amountSecure;
        jnportNotesInternal(amountNonSecure, resultlist -> daoNote.upsert(resultlist.toArray(new Note[]{})));

        boolean retval = true;
        if (reSecure) {
            Boolean[] done = new Boolean[]{false, false}; // [0] = done; [1] = success
            jnportNotesInternal(amountSecure, resultList -> {
                NoteConverter.BatchEncrypter.from(manager, context).setOnFinishListener(stream -> Executors.newSingleThreadExecutor().execute(() -> {
                    daoNote.upsert(stream.toArray(Note[]::new));
                    synchronized (Importer.this) {
                        done[0] = true;
                        done[1] = true;
                        Importer.this.notify();
                    }
                })).setOnErrorListener(error -> {
                    synchronized (Importer.this) {
                        done[0] = true;
                        done[1] = false;
                        Importer.this.notify();
                        if (migrationInterface != null)
                            ThreadUtil.runOnUIThread(() -> migrationInterface.onFatalError(error));
                    }
                }).encrypt(resultList);
            });
            synchronized (Importer.this) {
                try {
                    while (!done[0])
                        this.wait();
                } catch (InterruptedException e) {
                    Log.e("Importer", "Big big async trouble (jnport_notes):", e);
                }
            }
            retval = done[1];
        }
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onFinishNotes);
        return retval;
    }

    protected void jnportAnniversarys(long amount) {
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onStartAnniversarys);

        DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
        for (int x = 0; x < amount; ++x) {
            Anniversary m = Anniversary.template();
            visit(m);
            daoAnniversary.upsert(m);
        }

        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onFinishAnniversarys);
    }


    public void jnport(@NonNull Uri location, boolean reSecure) {
        ContentResolver contentResolver = context.getContentResolver();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                unpacker = MessagePack.newDefaultUnpacker(contentResolver.openInputStream(location));
                if (!MigrationUtil.checkHeader(unpacker)) {
                    if (migrationInterface != null)
                        ThreadUtil.runOnUIThread(() -> migrationInterface.onFatalError("Got a header mismatch. Is given file really from this app?"));
                    unpacker.close();
                    return;
                }
                long amountCategories = unpacker.unpackLong();
                long amountNotes = unpacker.unpackLong();
                long amountSecureNotes = unpacker.unpackLong();
                long amountAnniversarys = unpacker.unpackLong();

                long adjustedAmountNotes = reSecure ? amountNotes : amountNotes - amountSecureNotes;

                if (migrationInterface != null)
                    ThreadUtil.runOnUIThread(() -> migrationInterface.onStatsAvailable(amountCategories, adjustedAmountNotes, amountSecureNotes, amountAnniversarys));

                jnportCategories(amountCategories);

                boolean success = jnportNotes(manager, amountNotes, amountSecureNotes, reSecure);
                if (!success) {
                    unpacker.close();
                    return;
                }

                jnportAnniversarys(amountAnniversarys);

                unpacker.close();

                if (migrationInterface != null)
                    ThreadUtil.runOnUIThread(migrationInterface::onFinishMigration);
            } catch (IOException e) { //We get here if we cannot open file
                if (migrationInterface != null)
                    ThreadUtil.runOnUIThread(() -> migrationInterface.onFatalError("Cannot interact with file"));
            }
        });
    }
}
