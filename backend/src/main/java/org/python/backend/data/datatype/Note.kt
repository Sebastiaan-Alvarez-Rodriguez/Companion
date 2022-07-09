package org.python.backend.data.datatype

import android.graphics.Color
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.typeconverters.InstantConverter
import org.python.exim.EximUtil
import org.python.exim.Exportable
import org.python.exim.Importable
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
) : Exportable, Importable<Note> {

    constructor() : this(noteId = 0L, "", "", false, 0, date = Instant.MIN, renderType = RenderType.DEFAULT)

    override fun values(): Array<EximUtil.FieldInfo> =
        arrayOf(
            EximUtil.FieldInfo(noteId, "noteId"),
            EximUtil.FieldInfo(name, "name"),
            EximUtil.FieldInfo(content, "content"),
            EximUtil.FieldInfo(favorite, "favorite"),
            EximUtil.FieldInfo(securityLevel, "securityLevel"),
            EximUtil.FieldInfo(String(iv, Charsets.ISO_8859_1), "iv"),
            EximUtil.FieldInfo(InstantConverter.dateToTimestamp(date), "date"),
            EximUtil.FieldInfo(renderType.ordinal, "renderType"),
            EximUtil.FieldInfo(categoryKey, "categoryKey")
        )

    override val amountValues: Int = values().size

    override fun fromValues(values: List<Any?>): Note {
        return Note(
            noteId = values[0] as Long,
            name = values[1] as String,
            content = values[2] as String,
            favorite = values[3] as Boolean,
            securityLevel = values[4] as Int,
            iv = (values[5] as String).toByteArray(charset = Charsets.ISO_8859_1),
            date = InstantConverter.dateFromTimestamp(values[6] as Long)!!,
            renderType = RenderType.values()[values[7] as Int],
            categoryKey = values[8] as Long
        )
    }

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
        val EMPTY = Note()
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