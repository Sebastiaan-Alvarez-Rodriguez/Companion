package org.python.backend.data.datatype

import org.python.db.entities.RoomNote

data class Note(
    val id: Long = 0,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (other !is RoomNote)
            return false
        return id == other.id &&
                name == other.name &&
                content == other.content &&
                favorite == other.favorite &&
                secure == other.favorite
    }

    override fun hashCode(): Int {
        return this.id.toInt()
    }
}


