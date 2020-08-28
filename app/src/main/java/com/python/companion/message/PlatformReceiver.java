package com.python.companion.message;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.util.Log;

import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.dao.DAOMessage;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Message;
import com.python.companion.util.MessageUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Receiver object prepared by {@link Platform}.
 * Main purpose is to check for due {@link Message Messages}, send a notification for the due ones,
 * and reschedule the due ones
 */
public class PlatformReceiver extends BroadcastReceiver {
    /**
     * Returns all {@link Message} objects for which there are notifications due today or any moment before today
     * Caller is responsible for updating next notification time while handling the due items
     */
    private static @NonNull List<Message> getAnniversariesForNotifications(@NonNull Context context) {
        DAOMessage daoMessage = Database.getDatabase(context).getDAOMessage();
        return daoMessage.getDues();
    }

    /** Sets all due dates for due notifies to be on the first valid future date */
    private static void updateDueNotifies(List<Message> dues, Context context) {
        DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
        List<Anniversary> anniversaries = daoAnniversary.findByID(dues.stream().map(Message::getAnniversaryID));
        updateDueNotifies(dues, anniversaries, context);
    }

    /** Sets all due dates for due notifies to be on the first valid future date. Pass a list of dues and pointed anniversaries of equivalent size */
    private static void updateDueNotifies(List<Message> dues, List<Anniversary> dueAnniversaries, Context context) {
        if (dues.size() == 0 || dues.size() != dueAnniversaries.size())
            return;

        for (int x = 0; x < dues.size(); ++x) {
            Message due = dues.get(x);
            Anniversary m = dueAnniversaries.get(x);
            LocalDate d = due.getMessageDate(), now = LocalDate.now();
            long passed = m.between(d, now); // Number of anniversaries passed between notify- and boot time
            due.setMessageDate(d.plus(passed+1, m)); // Update next date to the first plausible time in the future
        }

        DAOMessage daoMessage = Database.getDatabase(context).getDAOMessage();
        daoMessage.update(dues.toArray(new Message[0])); // Schedules next notifies
    }

    /** Sends a notification to the Android system for a given anniversary and message object */
    private static void notify(Message notify, Anniversary anniversary, Context context) {
        Notification.Builder builder = new Notification.Builder(context, MessageUtil.getChannelID(anniversary));
        builder.setContentTitle(MessageUtil.getNotificationTitle(notify, anniversary))
                .setSmallIcon(R.drawable.ic_cactus_outline)
                .setLargeIcon(Icon.createWithResource(context, R.drawable.ic_cactus_companion_centered_small))
                .setStyle(new Notification.BigTextStyle().bigText(MessageUtil.getNotificationContent(notify, anniversary, context)));
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(MessageUtil.getNotificationID(anniversary), builder.build());
    }

    /** Triggered when phone has booted up */
    private static void onHandleBoot(List<Message> dues, Context context) {
        Platform.getPlatform(context).registerPlatformSchedule(context);

        DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
        List<Anniversary> anniversaries = daoAnniversary.findByID(dues.stream().map(Message::getAnniversaryID));

        for (int x = 0; x < dues.size(); ++x) {
            Message due = dues.get(x);
            Anniversary m = anniversaries.get(x);
            notify(due, m, context);
        }
        updateDueNotifies(dues, anniversaries, context);
    }

    /** Triggered when it is time to handle another cycle */
    private static void onHandleCycle(List<Message> dues, Context context) {
        DAOAnniversary daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
        List<Anniversary> anniversaries = daoAnniversary.findByID(dues.stream().map(Message::getAnniversaryID));
        for (int x = 0; x < dues.size(); ++x) {
            Message due = dues.get(x);
            Anniversary m = anniversaries.get(x);
            notify(due, m, context);
        }
        updateDueNotifies(dues, anniversaries, context);
    }

    /** Call to force a check */
    public static void manualCycle(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> onHandleCycle(getAnniversariesForNotifications(context), context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
        Boolean[] done = {false};
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Message> dues = getAnniversariesForNotifications(context);
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                onHandleBoot(dues, context);
            } else if (action.equals(context.getString(R.string.action_check_notifications)))
                onHandleCycle(dues, context);
            synchronized (PlatformReceiver.this) {
                done[0] = true;
                PlatformReceiver.this.notify();
            }
        });

        synchronized (PlatformReceiver.this) {
            try {
                while (!done[0])
                    this.wait();
            } catch (InterruptedException e) {
                Log.e("PR", "Big big async trouble (PR):", e);
            }
        }
    }
}
