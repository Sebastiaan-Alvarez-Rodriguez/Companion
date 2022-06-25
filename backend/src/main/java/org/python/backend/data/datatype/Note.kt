package org.python.backend.data.datatype

import android.graphics.Color
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.typeconverters.InstantConverter
import org.python.exim.ExportInfo
import org.python.exim.Exportable
import java.time.Instant

data class Note (
    val noteId: Long = 0L,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val securityLevel: Int,
    val iv: ByteArray = ByteArray(0),
    val date: Instant,
    val renderType: RenderType,
    val categoryKey: Long = -1L
) : Exportable {
    override fun values(): Array<ExportInfo> =
        arrayOf(
            ExportInfo(name, "name"),
            ExportInfo(content, "content"),
            ExportInfo(favorite, "favorite"),
            ExportInfo(securityLevel, "securityLevel"),
            ExportInfo(iv, "iv"),
            ExportInfo(InstantConverter.dateToTimestamp(date), "date"),
            ExportInfo(renderType.ordinal, "renderType"),
            ExportInfo(categoryKey, "categoryKey")
        )

    override fun equals(other: Any?): Boolean = other is Note && noteId == other.noteId &&
        name == other.name &&
        content == other.content &&
        favorite == other.favorite &&
        securityLevel == other.securityLevel &&
        iv.contentEquals(other.iv) &&
        date == other.date &&
        renderType == other.renderType &&
        categoryKey == other.categoryKey

    override fun hashCode(): Int = this.noteId.toInt()

    companion object {
        val EMPTY = Note(noteId = 0L, "", "", false, 0, date = Instant.MIN, renderType = RenderType.DEFAULT)
    }
}

enum class RenderType {
    DEFAULT, MARKDOWN, LATEX
}

data class NoteCategory(
    val categoryId: Long = 0,
    val name: String,
    val color: Color,
    val favorite: Boolean,
    val categoryDate: Instant
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
            favorite = RoomNoteCategory.DEFAULT.favorite,
            categoryDate = RoomNoteCategory.DEFAULT.categoryDate
        )
    }
}

data class NoteWithCategory(
    val note: Note,
    val noteCategory: NoteCategory
)