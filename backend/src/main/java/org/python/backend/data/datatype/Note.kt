package org.python.backend.data.datatype

data class Note(val id: Long = 0, val name: String, val content: String, val secure: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (other !is Note)
            return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.toInt()
    }
}


