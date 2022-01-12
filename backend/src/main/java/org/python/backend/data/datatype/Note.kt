package org.python.backend.data.datatype

data class Note(
    val id: Long = 0,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean,
    val iv: ByteArray = ByteArray(0)) {

    override fun equals(other: Any?): Boolean {
        if (other !is Note)
            return false
        return id == other.id &&
                name == other.name &&
                content == other.content &&
                favorite == other.favorite &&
                secure == other.secure &&
                iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        return this.id.toInt()
    }
}


