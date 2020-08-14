package com.python.companion.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.dao.DAONotify;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.entity.Notify;
import com.python.companion.util.NotificationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;

public class PlatformReceiver extends BroadcastReceiver {
    /**
     * Returns all {@link Notify} objects for which there are notifications due today or any moment before today
     * Caller is responsible for updating next notification time while handling the due items
     */
    private static @NonNull List<Notify> getJubileaForNotifications(@NonNull Context context) {
        DAONotify daoNotify = Database.getDatabase(context).getDAONotify();
        return daoNotify.getDues();
    }

    /** Sets all due dates for due notifies to be on the first valid future date */
    private static void updateDueNotifies(List<Notify> dues, Context context) {
        DAOMeasurement daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
        List<Measurement> measurements = daoMeasurement.findByID(dues.stream().map(Notify::getJubileumID));
        updateDueNotifies(dues, measurements, context);
    }
    /** Sets all due dates for due notifies to be on the first valid future date. Pass a list of dues and pointed measurements of equivalent size */
    private static void updateDueNotifies(List<Notify> dues, List<Measurement> dueMeasurements, Context context) {
        if (dues.size() == 0 || dues.size() != dueMeasurements.size())
            return;

        for (int x = 0; x < dues.size(); ++x) {
            Notify due = dues.get(x);
            Measurement m = dueMeasurements.get(x);
            LocalDate d = due.getNotifyDate(), now = LocalDate.now();
            long passed = m.between(d, now); // Number of jubilea passed between notify- and boot time
            due.setNotifyDate(d.plus(passed+1, m)); // Update next date to the first plausible time in the future
        }

        DAONotify daoNotify = Database.getDatabase(context).getDAONotify();
        daoNotify.update(dues.toArray(new Notify[0])); // Schedules next notifies
    }

    /** Sends a notification to the Android system for a given measurement and notify object */
    private static void notify(Notify notify, Measurement measurement, Context context) {
        Notification.Builder builder = new Notification.Builder(context, NotificationUtil.getChannelID(measurement));
        builder.setContentTitle(NotificationUtil.getNotificationTitle(notify, measurement));
        builder.setContentText(NotificationUtil.getNotificationContent(notify, measurement, context));
        builder.setSmallIcon(R.drawable.ic_cactus_outline);
        builder.setLargeIcon(Icon.createWithResource(context, R.drawable.ic_cactus_companion_centered));
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(NotificationUtil.getNotificationID(measurement), builder.build());
    }

    /** Triggered when phone has booted up */
    private static void onHandleBoot(List<Notify> dues, Context context) {
        Platform.getPlatform(context).registerPlatformSchedule(context);

        DAOMeasurement daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
        List<Measurement> measurements = daoMeasurement.findByID(dues.stream().map(Notify::getJubileumID));

        for (int x = 0; x < dues.size(); ++x) {
            Notify due = dues.get(x);
            Measurement m = measurements.get(x);
            notify(due, m, context);
        }
        updateDueNotifies(dues, measurements, context);
    }

    /** Triggered when it is time to handle another cycle */
    private static void onHandleCycle(List<Notify> dues, Context context) {
        DAOMeasurement daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
        List<Measurement> measurements = daoMeasurement.findByID(dues.stream().map(Notify::getJubileumID));

        for (int x = 0; x < dues.size(); ++x) {
            Notify due = dues.get(x);
            Measurement m = measurements.get(x);
            notify(due, m, context);
        }
        updateDueNotifies(dues, measurements, context);
    }

    /** Call to force a daily check. Note that any previously dismissed notifications of the user may appear again, potentially annoying users */
    public static void manualCycle(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> onHandleCycle(getJubileaForNotifications(context), context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Notify> dues = getJubileaForNotifications(context);
            if (action.equals(Intent.ACTION_BOOT_COMPLETED))
                onHandleBoot(dues, context);
            else if (action.equals(context.getString(R.string.action_check_notifications)))
                onHandleCycle(dues, context);
        });
    }
}
