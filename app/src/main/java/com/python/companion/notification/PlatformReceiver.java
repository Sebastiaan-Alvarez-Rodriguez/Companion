package com.python.companion.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Measurement;

import java.util.ArrayList;
import java.util.List;

public class PlatformReceiver extends BroadcastReceiver {

    /** Returns all jubilea for which there are notifications due today */
    private @NonNull List<Measurement> getJubileaForNotifications() {
        return new ArrayList<>();
    }

    /** Builds a single notification and sends it */
    private void notify(@NonNull Measurement measurement) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null)
            return;

        List<Measurement> measurements = getJubileaForNotifications();
        for (Measurement measurement : measurements)
            notify(measurement);
    }
}
