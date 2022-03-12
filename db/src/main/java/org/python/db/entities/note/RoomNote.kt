package org.python.db.entities.note

import androidx.room.*

@Entity(indices = [Index("name", unique = true)])
data class RoomNote(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean,
    val iv: ByteArray,
    val categoryKey: Long,
) {
    override fun equals(other: Any?): Boolean =  other is RoomNote && this.noteId == other.noteId
    override fun hashCode(): Int = this.noteId.hashCode()
}

data class RoomNoteWithCategory(
    @Embedded val note: RoomNote,
    @Relation(
        parentColumn = "categoryKey",
        entityColumn = "categoryId"
    )
    val noteCategory: RoomNoteCategory
)