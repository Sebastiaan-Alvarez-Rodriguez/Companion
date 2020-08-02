package com.python.companion.db.pojo.measurement;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Ignore;

import com.python.companion.db.entity.Measurement;
import com.python.companion.util.MeasurementUtil;

/**
 * Container to fetch a measurement with the singular and plural name of its parent, allowing livedata to trigger updates on parent name change
 */
public class MeasurementWithParentNames {
    @Embedded
    public Measurement measurement;

    public @Nullable String parentSingular;
    public @Nullable String parentPlural;


    public MeasurementWithParentNames() {}

    @Ignore
    public MeasurementWithParentNames(Measurement measurement, @Nullable String parentSingular, @Nullable String parentPlural) {
        this.measurement = measurement;
        this.parentSingular = parentSingular;
        this.parentPlural = parentPlural;
    }

    /** Fills missing information about parent singular or plural names */
    public void fill() {
        if (measurement != null && (parentSingular == null || parentPlural == null)) {
            long id = measurement.getParentID();
            this.parentSingular = MeasurementUtil.IDtoName(id, 1);
            this.parentPlural = MeasurementUtil.IDtoName(id, 2);
        }
    }
}
