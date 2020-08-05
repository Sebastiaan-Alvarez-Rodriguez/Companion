package com.python.companion.notification;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.python.companion.db.Database;
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.util.NotificationUtil;

import java.util.Calendar;

public class Platform {

    private @NonNull NotificationManager notificationManager;
    private @NonNull AlarmManager alarmManager;
    private @NonNull Database database;

    private static volatile Platform INSTANCE;

    public static Platform getPlatform(@NonNull Context context) {
        if (INSTANCE == null)
        synchronized (Platform.class) {
            if (INSTANCE == null)
                INSTANCE = new Platform(context);
        }
        return INSTANCE;
    }

    public Platform(@NonNull Context context) {
        this.notificationManager = context.getSystemService(NotificationManager.class);
        this.alarmManager = context.getSystemService(AlarmManager.class);
        this.database = Database.getDatabase(context);
    }

    /** Registers a notification channel for all currently known jubilea. Call this function on application startup */
    public void registerJubileaChannels() {
        MeasurementQuery measurementQuery = new MeasurementQuery(database);
        measurementQuery.getAll(list -> NotificationUtil.buildChannels(notificationManager, list));
    }

    private PendingIntent buildPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, PlatformReceiver.class);
//        intent.setAction("com.python.companion.PlatformReceiver"); //action = context.getString(R.string.action_notify_administer_medication)
        intent.setType("com.python.companion.PlatformReceiver");
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Setup main platform boot: Boots {@link PlatformReceiver} every day around approximately 00:00 */
    public void bootPlatform(@NonNull Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);


        PendingIntent pendingIntent = buildPendingIntent(context);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
