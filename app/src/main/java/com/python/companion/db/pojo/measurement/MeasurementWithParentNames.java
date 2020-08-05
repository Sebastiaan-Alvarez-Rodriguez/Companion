package com.python.companion.db.pojo.measurement;

import androidx.room.Embedded;
import androidx.room.Ignore;

import com.python.companion.db.entity.Measurement;

/**
 * Container to fetch a measurement with the singular and plural name of its parent, allowing livedata to trigger updates on parent name change
 */
public class MeasurementWithParentNames {
    @Embedded
    public Measurement measurement;

    public String parentSingular;
    public String parentPlural;


    public MeasurementWithParentNames() {}

    @Ignore
    public MeasurementWithParentNames(Measurement measurement, String parentSingular, String parentPlural) {
        this.measurement = measurement;
        this.parentSingular = parentSingular;
        this.parentPlural = parentPlural;
    }
}
