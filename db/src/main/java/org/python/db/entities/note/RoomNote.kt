package org.python.db.entities.note

import androidx.room.*

@Entity(indices = [Index("name", unique = true)])
data class RoomNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean,
    val iv: ByteArray,
    val categoryId: Long,
) {
    override fun equals(other: Any?): Boolean =  other is RoomNote && this.id == other.id
    override fun hashCode(): Int = this.id.hashCode()
}

data class RoomNoteWithCategory(
    @Embedded val note: RoomNote,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val roomNoteCategory: RoomNoteCategory?
)