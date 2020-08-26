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

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class Exporter implements EntityVisitor {
    protected MessagePacker packer;

    protected @NonNull FragmentManager manager;
    protected @NonNull Context context;
    protected @Nullable MigrationInterface migrationInterface;

    public static Exporter from(@NonNull FragmentManager manager, @NonNull Context context) {
        return new Exporter(manager, context);
    }

    protected Exporter(@NonNull FragmentManager manager, @NonNull Context context) {
        this.manager = manager;
        this.context = context;
        this.migrationInterface = null;
    }

    public Exporter with(@Nullable MigrationInterface migrationInterface) {
        this.migrationInterface = migrationInterface;
        return this;
    }

    @Override
    public void visit(@NonNull Category category) {
        try {
            packer.packString(category.getCategoryName());
            packer.packInt(category.getCategoryColor());
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onCategoryProcessed);
        } catch (IOException e) {
            Log.e("Exporter", "Big problem (note): ", e);
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onCategoryFailed);
        }
    }

    @Override
    public void visit(@NonNull Note note) {
        try {
            packer.packString(note.getName());
            packer.packString(note.getContent());
            packer.packString(note.getCategory().getCategoryName());
            packer.packInt(note.getCategory().getCategoryColor());
            packer.packLong(note.getModified().getEpochSecond());
            packer.packInt(note.getType());
            packer.packBoolean(note.isFavorite());
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onNoteProcessed);
        } catch (IOException e) {
            Log.e("Exporter", "Big problem (note): ", e);
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onNoteFailed);
        }
    }

    @Override
    public void visit(@NonNull Anniversary anniversary) {
        try {
            packer.packLong(anniversary.getAnniversaryID());
            packer.packString(anniversary.getNameSingular());
            packer.packString(anniversary.getNamePlural());
            packer.packString(anniversary.getDuration().toString());
            packer.packLong(anniversary.getAmount());
            packer.packLong(anniversary.getPrecomputedamount());
            packer.packLong(anniversary.getParentID());
            packer.packString(anniversary.getCornerstoneType().name());
//            packer.packBoolean(anniversary.hasNotifications()); TODO: Uncomment when ready
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onAnniversaryProcessed);
        } catch (IOException e) {
            Log.e("Exporter", "Big problem (anniversary): ", e);
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onNoteFailed);
        }
    }

    protected void exportCategories() {
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onStartCategories);

        DAOCategory daoCategory = Database.getDatabase(context).getDAOCategory();

        for (Category category : daoCategory.getAll())
            category.accept(this);

        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onFinishCategories);
    }

    protected boolean exportNotes(@NonNull FragmentManager manager, boolean skipSecure) {
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onStartNotes);

        DAONote daoNote = Database.getDatabase(context).getDAONote();
        for (Note note : daoNote.getInsecure())
            note.accept(this);
        if (skipSecure) {
            if (migrationInterface != null)
                ThreadUtil.runOnUIThread(migrationInterface::onFinishNotes);
            return true;
        }

        List<Note> secureNotes = daoNote.getSecure();

        Boolean[] done = new Boolean[]{false, false}; // [0] = done; [1] = success
        NoteConverter.BatchDecrypter.from(manager, context)
                .setOnErrorListener(error -> {
                    synchronized (Exporter.this) {
                        done[0] = true;
                        done[1] = false;
                        Exporter.this.notify();
                        if (migrationInterface != null)
                            ThreadUtil.runOnUIThread(() -> migrationInterface.onFatalError(error));
                    }
                })
                .setOnFinishListener(noteStream -> {
                    noteStream.forEach(note -> note.accept(this));
                    synchronized (Exporter.this) {
                        done[0] = true;
                        done[1] = true;
                        Exporter.this.notify();
                    }
                })
                .decrypt(secureNotes);
        synchronized (Exporter.this) {
            try {
                while (!done[0])
                    this.wait();
            } catch (InterruptedException e) {
                Log.e("Exporter", "Big big async trouble (export_notes):", e);
            }
        }
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onFinishNotes);
        return done[1];
    }

    protected void exportAnniversarys() {
        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onStartAnniversarys);

        DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();

        for (Anniversary anniversary : daoAnniversary.getAll())
            anniversary.accept(this);

        if (migrationInterface != null)
            ThreadUtil.runOnUIThread(migrationInterface::onFinishAnniversarys);
    }

    public void export(@NonNull Uri location, boolean skipSecure) {
        ContentResolver contentResolver = context.getContentResolver();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                packer = MessagePack.newDefaultPacker(contentResolver.openOutputStream(location));
                packer.packBinaryHeader(MigrationUtil.header.length);
                packer.writePayload(MigrationUtil.header);

                DAOCategory daoCategory = Database.getDatabase(context).getDAOCategory();
                long categoryAmount = daoCategory.count();
                packer.packLong(categoryAmount);

                DAONote daoNote = Database.getDatabase(context).getDAONote();
                long noteAmount = skipSecure ? daoNote.countInsecure() : daoNote.count();
                packer.packLong(noteAmount);
                long secureNoteAmount = skipSecure ? 0 : noteAmount - daoNote.countInsecure();
                packer.packLong(secureNoteAmount);

                DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
                long anniversaryAmount = daoAnniversary.count();
                packer.packLong(anniversaryAmount);

                if (migrationInterface != null)
                    ThreadUtil.runOnUIThread(()-> migrationInterface.onStatsAvailable(categoryAmount, noteAmount, secureNoteAmount, anniversaryAmount));

                exportCategories();

                boolean success = exportNotes(manager, skipSecure);

                if (!success) { // We had a fatal error. Stop prematurely, delete file and return
                    packer.close();
                    contentResolver.delete(location, null, null);
                    return;
                }

                exportAnniversarys();

                packer.close();

                if (migrationInterface != null)
                    ThreadUtil.runOnUIThread(migrationInterface::onFinishMigration);
            } catch (IOException e) { //We get here if we cannot open file
                if (migrationInterface != null)
                    ThreadUtil.runOnUIThread(() -> migrationInterface.onFatalError("Cannot interact with file"));
            }
        });
    }
}
