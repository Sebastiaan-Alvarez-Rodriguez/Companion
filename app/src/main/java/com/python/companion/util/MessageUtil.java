package com.python.companion.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Message;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to help sending messages, by managing notification channels and building notifications
 */
public class MessageUtil {
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
            if (title == null)
                throw new IllegalStateException("Must set a title (using builder.setTitle(title))!");
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_HEADER+title, title, importance);
            channel.setDescription(description);
            channel.setShowBadge(showBadge);
            return channel;
        }
    }

    /**
     *  Returns the channel identifier for all notifications of a given anniversary.
     * Note that this function does not guarantee the channel already has been created.
     * @implNote In current app, we force that all anniversaries get a channel as they are created, so we know certainly that channel exists
     */
    public static String getChannelID(@NonNull Anniversary anniversary) {
        return CHANNEL_ID_HEADER+ anniversary.getNamePlural();
    }

    /** Returns a unique identifier for all notifications of a given anniversary */
    public static int getNotificationID(@NonNull Anniversary anniversary) {
        return (int) anniversary.getAnniversaryID();
    }

    public static void buildChannel(@NonNull Anniversary anniversary, @NonNull Context context) {
        new ChannelBuilder().setTitle(anniversary.getNamePlural()).setDescription("Channel to receive "+ anniversary.getNameSingular()+" anniversaries on").build(context);
    }

    /**
     * Builds a notification for each provided anniversary
     * @param anniversaries List of anniversaries to build notification channels for. Channels are created in definition order in provided list
     */
    public static void buildChannels(@NonNull List<Anniversary> anniversaries, @NonNull Context context) {
        ChannelBuilder.buildAll(anniversaries.stream().map(anniversary -> new ChannelBuilder().setTitle(anniversary.getNamePlural()).setDescription("Channel to receive "+anniversary.getNameSingular()+" anniversaries on")).collect(Collectors.toList()), context);
    }

    /** Equivalent to {@link #buildChannels(List, Context)}. This call does not require context */
    public static void buildChannels(@NonNull List<Anniversary> anniversaries, @NonNull NotificationManager manager) {
        ChannelBuilder.buildAll(anniversaries.stream().map(anniversary -> new ChannelBuilder().setTitle(anniversary.getNamePlural()).setDescription("Channel to receive "+anniversary.getNameSingular()+" anniversaries on")).collect(Collectors.toList()), manager);
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
     * Deletes notification channel for given anniversary, if it exists.
     * NOTE: If you delete a notification channel with a given title, and later recreate it, it is configured with settings from before deletion!
     */
    public static void deleteChannel(@NonNull Anniversary anniversary, @NonNull Context context) {
        deleteChannel(anniversary.getNamePlural(), context);
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
     * Equivalent to {@link #deleteChannels(List, Context)}, when converting given anniversaries to their plural titles.
     * Other name is needed because of method erasure equivalence
     */
    public static void deleteChannels2(@NonNull List<Anniversary> anniversaries, @NonNull Context context) {
        deleteChannels(anniversaries.parallelStream().map(Anniversary::getNamePlural).collect(Collectors.toList()), context);
    }
    /** Equivalent to {@link #deleteChannels2(List, Context)}, using a given {@link NotificationManager} instead of a <code>Context</code> */
    public static void deleteChannels2(@NonNull List<Anniversary> anniversaries, @NonNull NotificationManager notificationManager) {
        deleteChannels(anniversaries.parallelStream().map(Anniversary::getNamePlural).collect(Collectors.toList()), notificationManager);
    }


    /** Returns <code>true</code> if this app has registered a notification channel with this title, <code>false</code> otherwise */
    public static boolean channelExists(@NonNull String title, @NonNull Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        for (NotificationChannel channel : notificationManager.getNotificationChannels())
            if (channel.getId().equals(CHANNEL_ID_HEADER+title))
                return true;
        return false;
    }


    /** Function to generate a notification title for given notify and anniversary */
    public static String getNotificationTitle(@NonNull Message notify, @NonNull Anniversary anniversary) {
        if (notify.getAmount() == 0) // We have an anniversary right now
            return anniversary.getNameSingular()+" Anniversary";
        else
            return anniversary.getNameSingular()+" Anniversary inbound";
    }

    /** Function to generate notification content for given message and anniversary */
    public static String getNotificationContent(@NonNull Message message, @NonNull Anniversary anniversary, @NonNull Context context) {
        long anniversariesHad = AnniversaryUtil.distanceCurrent(anniversary, AnniversaryUtil.getTogether(context));
        Anniversary expression = AnniversaryUtil.getBaseAnniversary(message.getType()); // base anniversary used to give distance in
        LocalDate anniversaryDate = message.getMessageDate().plus(message.getAmount(), expression);
        if (message.getAmount() == 0) { // We have an anniversary right now!
            if (anniversaryDate.isEqual(LocalDate.now())) {
                return "Today is your "+anniversariesHad + AnniversaryUtil.getDayOfMonthSuffix((int) anniversariesHad)+" "+ anniversary.getNameSingular()+" anniversary, congratulations!";
            } else {
                long dayDistance = ChronoUnit.DAYS.between(anniversaryDate, LocalDate.now());
                return "While you were gone: "+dayDistance+" "+(dayDistance == 1 ? "day" : "days")+" ago was your "+anniversariesHad + AnniversaryUtil.getDayOfMonthSuffix((int) anniversariesHad)+" "+ anniversary.getNameSingular()+" anniversary, congratulations!";
            }
        } else {
            long between = expression.between(LocalDate.now(), anniversaryDate); // Amount of units to the next anniversary (rounded down)
            if (between <= 0)
                return (-between) +" "+ ((-between) == 1 ? expression.getNameSingular() : expression.getNamePlural()) + " ago, you had your " + anniversariesHad + AnniversaryUtil.getDayOfMonthSuffix((int) anniversariesHad) + " " + anniversary.getNameSingular() + " anniversary, but we could not reach you in time!";
            return "In " + between + " " + (between == 1 ? expression.getNameSingular() : expression.getNamePlural()) + ", you will have your " + anniversariesHad + AnniversaryUtil.getDayOfMonthSuffix((int) anniversariesHad) + " " + anniversary.getNameSingular() + " anniversary!";

        }
    }
}
