package com.python.companion.message;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.db.Database;
import com.python.companion.db.constant.AnniversaryQuery;
import com.python.companion.util.MessageUtil;

import java.util.Calendar;

/**
 * Object to setup all tasks that are executed when this app is not running.
 * Mainly, notification channels are setup, and our {@link PlatformReceiver} get ready for action.
 *
 * Notification handling is inspired by: https://www.raywenderlich.com/1214490-android-notifications-tutorial-getting-started
 */
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

    /** Registers a notification channel for all currently known anniversaries. Call this function on application startup */
    public void registerAnniversariesChannels() {
        AnniversaryQuery anniversaryQuery = new AnniversaryQuery(database);
        anniversaryQuery.getAll(list -> MessageUtil.buildChannels(list, notificationManager));
    }

    private PendingIntent buildPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, PlatformReceiver.class);
        intent.setAction(context.getString(R.string.action_check_notifications));
        intent.setType("com.python.companion.platformreceiver");
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Setup main platform schedule: Boots {@link PlatformReceiver} four times every day approximately */
    public void registerPlatformSchedule(@NonNull Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        PendingIntent pendingIntent = buildPendingIntent(context);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HOUR * 6, pendingIntent);
    }
}