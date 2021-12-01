package org.python.backend.datatype

import android.os.Parcelable

data class Note(val name: String, val content: String)
data class NoteParcel(val member: Note) : Parcel<Note>(member) {
    private constructor(parcel: android.os.Parcel) : this(Note(name = parcel.readString().orEmpty(), content = parcel.readString().orEmpty()))

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) = CREATOR.writeToParcel(member, parcel)

    companion object CREATOR : Parcelable.Creator<NoteParcel> {
        fun writeToParcel(note: Note, parcel: android.os.Parcel) {
            parcel.writeString(note.name)
            parcel.writeString(note.content)
        }
        override fun createFromParcel(parcel: android.os.Parcel): NoteParcel = NoteParcel(parcel)
        override fun newArray(size: Int): Array<NoteParcel?> = newArray(size)
    }
}

data class NoteContext(val note: Note, val shouldSecure: Boolean)
data class NoteContextParcel(val member: NoteContext) : Parcel<NoteContext>(member) {
    private constructor(parcel: android.os.Parcel) : this(NoteContext(
        note = NoteParcel.createFromParcel(parcel).member,
        shouldSecure = parcel.readBoolean()))

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) = CREATOR.writeToParcel(member, parcel)

    companion object CREATOR : Parcelable.Creator<NoteContextParcel> {
        fun writeToParcel(noteContext: NoteContext, parcel: android.os.Parcel) {
            NoteParcel.writeToParcel(noteContext.note, parcel)
            parcel.writeBoolean(noteContext.shouldSecure)
        }
        override fun createFromParcel(parcel: android.os.Parcel): NoteContextParcel = NoteContextParcel(parcel)
        override fun newArray(size: Int): Array<NoteContextParcel?> = newArray(size)
    }
}


