package com.python.companion.db.interact;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.Database;
import com.python.companion.db.constant.AnniversaryQuery;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.dao.DAOMessage;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Message;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;
import com.python.companion.ui.anniversary.dialog.AnniversaryDeleteDialog;
import com.python.companion.util.MessageUtil;
import com.python.companion.util.genericinterfaces.FinishListener;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.List;
import java.util.concurrent.Executors;


public class AnniversaryStore {
    /**
     * Insert a given new anniversary into the database, either when there is no name-conflict, or the user tells us we may override
     * @param anniversary Anniversary to store. If its {@code secure} field is set, store the anniversary securely
     * @param callback Callback receives a call in either {@link AnniversaryStore.StoreCallback#onSuccess()) if we stored the new anniversary, or {@link AnniversaryStore.StoreCallback#onFailure()} if we did not
     */
    public static void insert(@NonNull Anniversary anniversary, @NonNull FragmentManager manager, @NonNull Context context, @NonNull AnniversaryStore.StoreCallback callback) {
        final AnniversaryQuery anniversaryQuery = new AnniversaryQuery(context);
        DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
        anniversaryQuery.isUniqueInstancedNamed(anniversary.getNameSingular(), anniversary.getNamePlural(), result -> {
            if (result == null) { // Unique-named anniversary
                _insert(daoAnniversary, anniversary);
                MessageUtil.buildChannel(anniversary, context);
                callback.onSuccess();
            } else { // Another anniversary with same name exists
                showDeleteDialog(result, manager, new AnniversaryStore.StoreCallback() {
                    @Override
                    public void onSuccess() {
                        _insert(daoAnniversary, anniversary);
                        MessageUtil.buildChannel(anniversary, context);
                        callback.onSuccess();
                    }
                    @Override
                    public void onFailure() {
                        callback.onFailure();
                    }
                });
            }
        });
    }

    /**
     * Update given anniversary, either when there is no change in names, there is no name conflict, or when the user tells us we may override
     * @param anniversary Anniversary to update. If its {@code secure} field is set, store the anniversary securely
     * @param old Anniversary before changes
     * @param callback Callback receives a call in either {@link AnniversaryStore.StoreCallback#onSuccess()) if we updated the anniversary, or {@link AnniversaryStore.StoreCallback#onFailure()} if we did not
     */
    public static void update(@NonNull Anniversary anniversary, @NonNull Anniversary old, @NonNull FragmentManager manager, @NonNull Context context, @NonNull AnniversaryStore.StoreCallback callback) {
        final AnniversaryQuery anniversaryQuery = new AnniversaryQuery(context);
        if (old.getNameSingular().equals(anniversary.getNameSingular()) && old.getNamePlural().equals(anniversary.getNamePlural())) {
            updateInternal(Database.getDatabase(context).getDAOAnniversary(), Database.getDatabase(context).getDAOMessage(), context, anniversary, old, callback);
        } else {
            updateCheckSingular(anniversaryQuery, Database.getDatabase(context).getDAOAnniversary(), Database.getDatabase(context).getDAOMessage(), anniversary, old, manager, context, callback);
        }
    }

    public static void delete(@NonNull Anniversary anniversary, @NonNull Context context, @NonNull FinishListener listener) {
        MessageUtil.deleteChannel(anniversary, context);
        _delete(Database.getDatabase(context).getDAOAnniversary(), anniversary, listener);
    }

    public static void delete(@NonNull List<Anniversary> anniversaries, @NonNull Context context, @NonNull FinishListener listener) {
        MessageUtil.deleteChannels2(anniversaries, context);
        _delete(Database.getDatabase(context).getDAOAnniversary(), anniversaries, listener);
    }

    private static void updateInternal(@NonNull DAOAnniversary daoAnniversary, @NonNull DAOMessage daoMessage, @NonNull Context context, @NonNull Anniversary anniversary, @NonNull Anniversary old, @NonNull AnniversaryStore.StoreCallback callback) {
        _update(daoAnniversary, daoMessage, context, anniversary, old, success -> {
            if (success)
                callback.onSuccess();
            else
                callback.onFailure();
        });
    }

    private static void updateCheckSingular(@NonNull AnniversaryQuery anniversaryQuery, @NonNull DAOAnniversary daoAnniversary,  @NonNull DAOMessage daoMessage, @NonNull Anniversary anniversary, @NonNull Anniversary old, @NonNull FragmentManager manager, @NonNull Context context, @NonNull AnniversaryStore.StoreCallback callback) {
        if (!old.getNameSingular().equals(anniversary.getNameSingular())) { // Singular name changed
            anniversaryQuery.isUniqueInstancedNamed(anniversary.getNameSingular(), old.getNamePlural(), result -> {
                if (result == null || result.anniversary.getAnniversaryID() == anniversary.getAnniversaryID()) { // New name is unique
                    updateCheckPlural(anniversaryQuery, daoAnniversary, daoMessage, context, anniversary, old, manager, callback);
                } else { // Another anniversary with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            delete(anniversary, context, () -> updateCheckPlural(anniversaryQuery, daoAnniversary, daoMessage, context, anniversary, old, manager, callback));
                        }
                        @Override
                        public void onFailure() {
                            callback.onFailure();
                        }
                    });
                }
            });
        } else {
            updateCheckPlural(anniversaryQuery, daoAnniversary, daoMessage, context, anniversary, old, manager, callback);
        }
    }

    private static void updateCheckPlural(@NonNull AnniversaryQuery anniversaryQuery, @NonNull DAOAnniversary daoAnniversary,  @NonNull DAOMessage daoMessage, @NonNull Context context, @NonNull Anniversary anniversary, @NonNull Anniversary old, @NonNull FragmentManager manager, @NonNull AnniversaryStore.StoreCallback callback) {
        if (!old.getNamePlural().equals(anniversary.getNamePlural())) {// Plural name changed
            anniversaryQuery.isUniqueInstancedNamed(old.getNameSingular(), anniversary.getNamePlural(), result -> {
                if (result == null || result.anniversary.getAnniversaryID() == anniversary.getAnniversaryID()) { // New name is unique
                    updateInternal(daoAnniversary, daoMessage, context, anniversary, old, callback);
                } else { // Another anniversary with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            updateInternal(daoAnniversary, daoMessage, context, anniversary, old, callback);
                        }
                        @Override
                        public void onFailure() {
                            callback.onFailure();
                        }
                    });
                }
            });
        } else {
            updateInternal(daoAnniversary, daoMessage, context, anniversary, old, callback);
        }
    }

    private static void showDeleteDialog(@NonNull AnniversaryWithParentNames conflicting, @NonNull FragmentManager manager, @NonNull AnniversaryStore.StoreCallback callback) {
        AnniversaryDeleteDialog deleteDialog = new AnniversaryDeleteDialog.Builder()
                .setExistsText("Anniversary name already exists!")
                .setQuestionText("Do you want to delete existing anniversary?")
                .setWarningText("Warning: Deleted anniversaries cannot be restored")
                .setAnniversaryWithParentName(conflicting)
                .setDeleteListener(callback::onSuccess)
                .setCancelListener(callback::onFailure)
                .build();
        deleteDialog.show(manager, null);
    }

    /** Callback to receive final state of store operations */
    public interface StoreCallback {
        void onSuccess();
        void onFailure();
    }


    /** Insert a new anniversary. Do not call this directly. Instead, use {@link #insert(Anniversary, FragmentManager, Context, AnniversaryStore.StoreCallback)}*/
    private static void _insert(@NonNull DAOAnniversary daoAnniversary, @NonNull Anniversary anniversary) {
        Executors.newSingleThreadExecutor().execute(() -> daoAnniversary.insert(anniversary));
    }

    /** Deletes an anniversary. Do not call this directly. Instead, use {@link #delete(List, Context, FinishListener)} */
    private static void _delete(@NonNull DAOAnniversary daoAnniversary, @NonNull List<Anniversary> anniversaries, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (Anniversary m : anniversaries)
                daoAnniversary.deleteInherit(m);
            listener.onFinish();
        });
    }

    /** Deletes an anniversary. Do not call this directly. Instead, use {@link #delete(Anniversary, Context, FinishListener)} */
    private static void _delete(@NonNull DAOAnniversary daoAnniversary, @NonNull Anniversary anniversary, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoAnniversary.deleteInherit(anniversary);
            listener.onFinish();
        });
    }

    /** Updates an anniversary, handling inheritance. Do not call this directly. Instead, use {@link #update(Anniversary, Anniversary, FragmentManager, Context, AnniversaryStore.StoreCallback)} */
    private static void _update(@NonNull DAOAnniversary daoAnniversary, @NonNull DAOMessage daoMessage, @NonNull Context context, @NonNull Anniversary anniversary, @NonNull Anniversary old, @NonNull ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            @Nullable List<Anniversary> updated = daoAnniversary.updateInherit(anniversary, old);
            if (updated != null) {
                updated.parallelStream().filter(Anniversary::getHasNotifications).forEach(a -> {
                    List<Message> messages = daoMessage.getMessagesForAnniversary(a.getAnniversaryID());
                    for (int x = 0; x < messages.size(); ++x) {
                        Message old1 = messages.get(x);
                        Message cur = Message.from(context, a, old1.getAmount(), old1.getType());
                        cur.setMessageID(old1.getMessageID());
                        messages.set(x, cur);
                    }
                    daoMessage.update(messages.toArray(new Message[0]));
                });
            }
            listener.onResult(updated != null);
        });
    }
}
