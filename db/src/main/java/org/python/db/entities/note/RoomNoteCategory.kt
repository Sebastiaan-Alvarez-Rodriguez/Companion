package org.python.db.entities.note

import android.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(indices = [Index("categoryName", unique = true)])
data class RoomNoteCategory(
    @PrimaryKey(autoGenerate = true) val categoryId: Long = 0L,
    val categoryName: String,
    val categoryColor: Color,
    val categoryFavorite: Boolean,
    val categoryDate: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (other !is RoomNoteCategory)
            return false
        return this.categoryId == other.categoryId
    }

    override fun hashCode(): Int {
        return this.categoryId.hashCode()
    }

    companion object {
        val DEFAULT: RoomNoteCategory = RoomNoteCategory(
            categoryId = 0L,
            categoryName = "default",
            categoryColor = Color.valueOf(Color.WHITE),
            categoryFavorite = false,
            categoryDate = Instant.EPOCH
        )

        enum class SortableField {
            NAME,
            DATE
        }
    }
}