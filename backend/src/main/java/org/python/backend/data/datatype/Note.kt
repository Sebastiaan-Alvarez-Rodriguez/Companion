package org.python.backend.data.datatype

import android.graphics.Color
import org.python.db.entities.note.RoomNoteCategory

data class Note(
    val noteId: Long = 0L,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val securityLevel: Int,
    val iv: ByteArray = ByteArray(0),
    val categoryKey: Long = -1L
) {
    override fun equals(other: Any?): Boolean = other is Note && noteId == other.noteId &&
        name == other.name &&
        content == other.content &&
        favorite == other.favorite &&
        securityLevel == other.securityLevel &&
        iv.contentEquals(other.iv) &&
        categoryKey == other.categoryKey

    override fun hashCode(): Int = this.noteId.toInt()
}

data class NoteCategory(
    val categoryId: Long = 0,
    val name: String,
    val color: Color,
    val favorite: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (other !is NoteCategory)
            return false
        return categoryId == other.categoryId && name == other.name && color == other.color && favorite == other.favorite
    }

    override fun hashCode(): Int = this.categoryId.toInt()

    companion object {
        val DEFAULT: NoteCategory = NoteCategory(
            categoryId = RoomNoteCategory.DEFAULT.categoryId,
            name = RoomNoteCategory.DEFAULT.categoryName,
            color = RoomNoteCategory.DEFAULT.color,
            favorite = RoomNoteCategory.DEFAULT.favorite
        )
    }
}

data class NoteWithCategory(
    val note: Note,
    val noteCategory: NoteCategory
)