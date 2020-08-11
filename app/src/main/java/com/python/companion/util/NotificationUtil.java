package com.python.companion.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.db.entity.Measurement;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to manage notification channels and build notifications
 *
 * https://www.raywenderlich.com/1214490-android-notifications-tutorial-getting-started
 * Idea: Build a table JubileumNotification, with:
 * 1. reference to Measurement ID it belongs to
 * 2. the date and time at which notification must appear
 * 3. the date at which jubileum actually is
 * 4. Optionally, the text to display, although this can also be determined at runtime
 * 5. Optionally, hint for runtime to setup pendingintent
 * We would use this table by waking our app up daily, and then check what notifications are scheduled during the day, and handle them
 *
 *
 * NOTE: I found out that we can schedule repeating using milliseconds, as long/int format.
 * So we have a period of 2^31 -1 ms = approx 24855 days = approx 68 years in which we can define our repeat period very exactly
 * We could then simply schedule on alarm to build a notification directly, passing any information along in intent extras
 * (Cancel an AlarmManager: https://stackoverflow.com/questions/3330522/)
 * To cancel a pending alarm, we must recreate the intent <code>PendingIntent.getBroadcast()</code>
 * We must make a table which maps Measurement ID to the broadcast ID. Also, Measurement ID must map to the notification ID.
 * That way, we can cancel both alarmmanager and any notification IDs.
 * Still, we would need a table, so users can see and change their notification data
 */
public class NotificationUtil {
    protected static final CharSequence CHANNEL_ID_HEADER = "com.python.companion.";

    /**
     * Simple builder to build a notification channel
     */
    public static class ChannelBuilder {
        protected @Nullable String title, description;

        protected int importance;
        protected boolean showBadge;

        public ChannelBuilder() {
            this.importance = NotificationManager.IMPORTANCE_DEFAULT;
        }

        public ChannelBuilder setTitle(@NonNull String title) {
            this.title = title;
            return this;
        }

        public ChannelBuilder setDescription(@NonNull String description) {
            this.description = description;
            return this;
        }

        /** @see NotificationManager for possible importances (e.g. <code>IMPORTANCE_DEFAULT, IMPORTANCE_HIGH</code> */
        public ChannelBuilder setImportance(int importance) {
            this.importance = importance;
            return this;
        }

        public ChannelBuilder setShowBadge(boolean showBadge) {
            this.showBadge = showBadge;
            return this;
        }

        /** Builds and registers the notification channel. If notification channel already exists, nothing happens */
        public NotificationChannel build(@NonNull Context context) {
            NotificationChannel channel = reap();
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            return channel;
        }

        /** Builds and registers all given prepared channelBuilders */
        public static void buildAll(@NonNull Collection<ChannelBuilder> channelBuilders, @NonNull Context context) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannels(channelBuilders.parallelStream().map(ChannelBuilder::reap).collect(Collectors.toList()));
        }
        /** Equivalent to {@link #buildAll(Collection, Context)}. This call does not require context, but instead a NotificationManager */
        public static void buildAll(@NonNull Collection<ChannelBuilder> channelBuilders, @NonNull NotificationManager manager) {
            manager.createNotificationChannels(channelBuilders.parallelStream().map(ChannelBuilder::reap).collect(Collectors.toList()));
        }

        /**
         * Only returns the notification channel, without registering it. NOTE: You cannot send notifications to unregistered channels.
         * Use this function to chain building of notification channels, in order to register many at once
         * @return An <strong>unregistered</strong> NotificationChannel, which should be registered by caller
         */
        @CheckResult
        protected NotificationChannel reap() {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_HEADER+title, title, importance);
            channel.setDescription(description);
            channel.setShowBadge(showBadge);
            return channel;
        }
    }


    public static void buildChannel(@NonNull Measurement measurement, @NonNull Context context) {
        new ChannelBuilder().setTitle(measurement.getNamePlural()).setDescription("Channel to receive "+measurement.getNameSingular()+" jubilea on").build(context);
    }

    /**
     * Builds a notification for each provided measurement
     * @param measurements List of measurements to build notification channels for. Channels are created in definition order in provided list
     */
    public static void buildChannels(@NonNull List<Measurement> measurements, @NonNull Context context) {
        ChannelBuilder.buildAll(measurements.stream().map(measurement -> new ChannelBuilder().setTitle(measurement.getNamePlural()).setDescription("Channel to receive "+measurement.getNameSingular()+" jubilea on")).collect(Collectors.toList()), context);
    }

    /** Equivalent to {@link #buildChannels(List, Context)}. This call does not require context */
    public static void buildChannels(@NonNull List<Measurement> measurements, @NonNull NotificationManager manager) {
        ChannelBuilder.buildAll(measurements.stream().map(measurement -> new ChannelBuilder().setTitle(measurement.getNamePlural()).setDescription("Channel to receive "+measurement.getNameSingular()+" jubilea on")).collect(Collectors.toList()), manager);
    }

    /**
     * Deletes notification channel with given name, if it exists.
     * NOTE: If you delete a notification channel with a given title, and later recreate it, it is configured with settings from before deletion!
     * @param channelTitle Title of notification channel
     */
    protected static void deleteChannel(@NonNull String channelTitle, @NonNull Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.deleteNotificationChannel(CHANNEL_ID_HEADER+channelTitle);
    }

    /**
     * Deletes notification channel for given measurement, if it exists.
     * NOTE: If you delete a notification channel with a given title, and later recreate it, it is configured with settings from before deletion!
     */
    public static void deleteChannel(@NonNull Measurement measurement, @NonNull Context context) {
        deleteChannel(measurement.getNamePlural(), context);
    }

    /**
     * Deletes list of notification channels
     * NOTE: If you delete a notification channel with a given title, and later recreate it, it is configured with settings from before deletion!
     */
    protected static void deleteChannels(@NonNull List<String> titles, @NonNull Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        deleteChannels(titles, notificationManager);
    }

    /** Equivalent to {@link #deleteChannels(List, Context)}, using a given {@link NotificationManager} instead of a <code>Context</code> */
    public static void deleteChannels(@NonNull List<String> titles, @NonNull NotificationManager notificationManager) {
        for (String title : titles)
            notificationManager.deleteNotificationChannel(CHANNEL_ID_HEADER+title);
    }

    /**
     * Equivalent to {@link #deleteChannels(List, Context)}, when converting given measurements to their plural titles.
     * Other name is needed because of method erasure equivalence
     */
    public static void deleteChannels2(@NonNull List<Measurement> measurements, @NonNull Context context) {
        deleteChannels(measurements.parallelStream().map(Measurement::getNamePlural).collect(Collectors.toList()), context);
    }
    /** Equivalent to {@link #deleteChannels2(List, Context)}, using a given {@link NotificationManager} instead of a <code>Context</code> */
    public static void deleteChannels2(@NonNull List<Measurement> measurements, @NonNull NotificationManager notificationManager) {
        deleteChannels(measurements.parallelStream().map(Measurement::getNamePlural).collect(Collectors.toList()), notificationManager);
    }


    /** Returns <code>true</code> if this app has registered a notification channel with this title, <code>false</code> otherwise */
    public static boolean channelExists(@NonNull String title, @NonNull Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        for (NotificationChannel channel : notificationManager.getNotificationChannels())
            if (channel.getId().equals(CHANNEL_ID_HEADER+title))
                return true;
        return false;
    }


    /**
     * Constructs builder object to create notifications
     * @see Notification.Builder
     * @param channelTitle Title of the channel you want to post this notification on
     * @return builder object to create notifications
     */
    public static Notification.Builder builder(@NonNull String channelTitle, @NonNull Context context) {
        return new Notification.Builder(context, CHANNEL_ID_HEADER+channelTitle);
    }
}
