package org.python.db.entities.note

import androidx.room.*
import java.time.Instant

@Entity(indices = [Index("name", unique = true)])
data class RoomNote(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val securityLevel: Int,
    val iv: ByteArray,
    val date: Instant,
    val renderType: Int,
    val categoryKey: Long,
) {
    override fun equals(other: Any?): Boolean = other is RoomNote && this.noteId == other.noteId
    override fun hashCode(): Int = this.noteId.hashCode()
}

data class RoomNoteWithCategory(
    @Embedded val note: RoomNote,
    @Relation(
        parentColumn = "categoryKey",
        entityColumn = "categoryId",
        entity = RoomNoteCategory::class
    )
    val noteCategory: RoomNoteCategory
) {
    companion object {
        enum class SortableField {
            NAME,
            DATE,
            CATEGORYNAME,
            SECURITYLEVEL
        }
    }
}