package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.List;
import java.util.concurrent.Executors;

public class AnniversaryQuery {
    private DAOAnniversary daoAnniversary;

    public AnniversaryQuery(@NonNull Context context) {
        daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
    }

    public AnniversaryQuery(@NonNull Database database) {
        daoAnniversary = database.getDAOAnniversary();
    }

    public void getAll(@NonNull ResultListener<List<Anniversary>> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoAnniversary.getAll()));
    }

    public void setHasNotifications( boolean hasNotifications, long anniversaryID) {
        Executors.newSingleThreadExecutor().execute(() -> daoAnniversary.setHasNotifications(anniversaryID, hasNotifications));
    }

    public void isUnique(String nameSingular, String namePlural, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoAnniversary.getBySingularOrPlural(nameSingular, namePlural) == null));
    }

    public void isUniqueInstanced(String nameSingular, String namePlural, ResultListener<Anniversary> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoAnniversary.getBySingularOrPlural(nameSingular, namePlural)));
    }

    public void isUniqueInstancedNamed(String nameSingular, String namePlural, ResultListener<AnniversaryWithParentNames> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoAnniversary.getBySingularOrPluralNamed(nameSingular, namePlural)));
    }

    public void findByID(long id, ResultListener<Anniversary> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoAnniversary.findByID(id)));
    }

    public void findByIDNamed(long id, ResultListener<AnniversaryWithParentNames> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoAnniversary.findByIDNamed(id)));
    }

    @WorkerThread
    public @Nullable
    Anniversary isUniqueInstanced(String nameSingular, String namePlural) {
        return daoAnniversary.getBySingularOrPlural(nameSingular, namePlural);
    }
}
