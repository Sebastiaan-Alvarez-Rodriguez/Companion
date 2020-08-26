package com.python.companion.db.pojo.anniversary;

import androidx.room.Embedded;
import androidx.room.Ignore;

import com.python.companion.db.entity.Anniversary;

/**
 * Container to fetch an anniversary with the singular and plural name of its parent, allowing livedata to trigger updates on parent name change
 */
public class AnniversaryWithParentNames {
    @Embedded
    public Anniversary anniversary;

    public String parentSingular;
    public String parentPlural;


    public AnniversaryWithParentNames() {}

    @Ignore
    public AnniversaryWithParentNames(Anniversary anniversary, String parentSingular, String parentPlural) {
        this.anniversary = anniversary;
        this.parentSingular = parentSingular;
        this.parentPlural = parentPlural;
    }
}
