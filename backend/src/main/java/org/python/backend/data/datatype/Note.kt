package org.python.backend.data.datatype

import android.graphics.Color

data class Note(
    val id: Long = 0L,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean,
    val iv: ByteArray = ByteArray(0),
    val categoryId: Long = -1L
) {
    override fun equals(other: Any?): Boolean = other is Note && id == other.id &&
        name == other.name &&
        content == other.content &&
        favorite == other.favorite &&
        secure == other.secure &&
        iv.contentEquals(other.iv) &&
        categoryId == other.categoryId

    override fun hashCode(): Int = this.id.toInt()
}

data class NoteCategory(
    val id: Long = 0,
    val name: String,
    val color: Color
) {
    override fun equals(other: Any?): Boolean {
        if (other !is NoteCategory)
            return false
        return id == other.id && name == other.name && color == other.color
    }

    override fun hashCode(): Int = this.id.toInt()
}
