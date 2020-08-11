package com.python.companion.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.dao.DAONotify;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.entity.Notify;

import java.time.LocalDate;
import java.util.List;

public class PlatformReceiver extends BroadcastReceiver {

    /** Builds a single notification and sends it */
    private void notify(@NonNull Notify notify) {

    }

    /**
     * Returns all {@link Notify} objects for which there are notifications due today or any moment before today
     * Caller is responsible for updating next notification time while handling the due items
     */
    private @NonNull List<Notify> getJubileaForNotifications(@NonNull Context context) {
        DAONotify daoNotify = Database.getDatabase(context).getDAONotify();
        return daoNotify.getDues();
    }

    /** Sets all due dates for due notifies to be on the first valid future date */
    private void updateDueNotifies(List<Notify> dues, Context context) {
        if (dues.size() == 0)
            return;
        DAOMeasurement daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
        List<Measurement> measurements = daoMeasurement.findByID(dues.stream().map(Notify::getJubileumID));
        for (int x = 0; x < dues.size(); ++x) {
            Notify due = dues.get(x);
            Measurement m = measurements.get(x);
            LocalDate d = due.getNotifyDate(), now = LocalDate.now();
            long passed = m.between(d, now); // Number of jubilea passed between notify- and boot time
            due.setNotifyDate(d.plus(passed+1, m)); // Update next date to the first plausible time in the future
        }

        DAONotify daoNotify = Database.getDatabase(context).getDAONotify();
        daoNotify.update(dues.toArray(new Notify[0])); // Schedules next notifies
    }

    /** Triggered when phone has booted up */
    private void onHandleBoot(List<Notify> dues, Context context) {
        Platform.getPlatform(context).bootPlatform(context);
        updateDueNotifies(dues, context);
    }

    /** Triggered when it is time to handle another cycle */
    private void onHandleCycle(List<Notify> dues, Context context) {
        //TODO: Is androidmanifest filter required for our custom action?
        // Look at https://developer.android.com/guide/components/broadcasts#java
        updateDueNotifies(dues, context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;

        List<Notify> dues = getJubileaForNotifications(context);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED))
            onHandleBoot(dues, context);
        else if (action.equals(context.getString(R.string.action_check_notifications)))
            onHandleCycle(dues, context);
    }
}
