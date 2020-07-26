package com.python.companion.db.pojo.measurement;

import androidx.annotation.Nullable;
import androidx.room.Embedded;

import com.python.companion.db.entity.Measurement;

/**
 * Container to fetch a measurement with the singular and plural name of its parent, allowing livedata to trigger updates on parent name change
 */
public class MeasurementWithParentNames {
    @Embedded
    public Measurement measurement;
//    @ColumnInfo(name = "nameSingular")
    public @Nullable String parentSingular;
//    @ColumnInfo(name = "namePlural")
    public @Nullable String parentPlural;
}
