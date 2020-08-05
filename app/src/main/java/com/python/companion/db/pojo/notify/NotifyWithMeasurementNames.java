package com.python.companion.db.pojo.notify;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Ignore;

import com.python.companion.db.entity.Notify;

/**
 * Container to fetch a measurement with the singular and plural name of its parent, allowing livedata to trigger updates on parent name change
 */
public class NotifyWithMeasurementNames {
    @Embedded
    public Notify notify;

    public @Nullable String measurementSingular;
    public @Nullable String measurementPlural;


    public NotifyWithMeasurementNames() {}

    @Ignore
    public NotifyWithMeasurementNames(Notify notify, @Nullable String parentSingular, @Nullable String parentPlural) {
        this.notify = notify;
        this.measurementSingular = parentSingular;
        this.measurementPlural = parentPlural;
    }
}
