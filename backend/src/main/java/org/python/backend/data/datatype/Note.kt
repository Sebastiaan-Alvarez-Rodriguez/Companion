package org.python.backend.data.datatype

import android.graphics.Color

data class Note(
    val noteId: Long = 0L,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean,
    val iv: ByteArray = ByteArray(0),
    val categoryKey: Long = -1L
) {
    override fun equals(other: Any?): Boolean = other is Note && noteId == other.noteId &&
        name == other.name &&
        content == other.content &&
        favorite == other.favorite &&
        secure == other.secure &&
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
            name = "",
            color = Color.valueOf(Int.MAX_VALUE),// TODO get default note color style from styles
            favorite = false
        )
    }
}

data class NoteWithCategory(
    val note: Note,
    val noteCategory: NoteCategory
)