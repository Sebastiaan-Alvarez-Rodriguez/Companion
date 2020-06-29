package com.python.companion.ui.notes.note;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;

import java.time.Instant;

/** Class wrapping around a {@link Note}, allowing it to be sent between Activities via Parcels ({@link Parcel}) */
public class NoteContainer implements Parcelable {
    private @NonNull Note note;

    public NoteContainer(@NonNull Note note) {
        this.note = note;
    }

    protected NoteContainer(@NonNull Parcel in) {
        String name = in.readString(), content = in.readString();
        Category c = new Category(in.readString(), in.readInt());
        boolean secure = in.readBoolean();
        int arrlen = in.readInt();
        byte[] iv;
        if (arrlen > 0) {
            iv = new byte[arrlen];
            in.readByteArray(iv);
        } else {
            iv = null;
        }
        Instant modified = Instant.ofEpochSecond(in.readLong());
        int type = in.readInt();
        boolean favorite = in.readBoolean();
        note = new Note(name, content, c, secure, iv, modified, type, favorite);
    }

    public @NonNull Note getNote() {
        return note;
    }


    public static final Creator<NoteContainer> CREATOR = new Creator<NoteContainer>() {
        @Override
        public NoteContainer createFromParcel(Parcel in) {
            return new NoteContainer(in);
        }

        @Override
        public NoteContainer[] newArray(int size) {
            return new NoteContainer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(note.getName());
        dest.writeString(note.getContent());
        dest.writeString(note.getCategory().getCategoryName());
        dest.writeInt(note.getCategory().getCategoryColor());
        dest.writeBoolean(note.isSecure());
        if (note.getIv() == null) {
            dest.writeInt(0);
        } else {
            dest.writeInt(note.getIv().length);
            dest.writeByteArray(note.getIv());
        }
        dest.writeLong(note.getModified().getEpochSecond());
        dest.writeInt(note.getType());
        dest.writeBoolean(note.isFavorite());
    }
}
