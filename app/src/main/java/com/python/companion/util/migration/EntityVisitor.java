package com.python.companion.util.migration;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;

/**
 * Interface allowing implementing class to visit database entities
 */
public interface EntityVisitor {
    void visit(@NonNull Category category);
    void visit(@NonNull Note note);
    void visit(@NonNull Anniversary anniversary);

    /** Interface allowing implementing class to be visited by classes implementing {@link EntityVisitor} */
    interface Visitable {
        void accept(@NonNull EntityVisitor visitor);
    }
}