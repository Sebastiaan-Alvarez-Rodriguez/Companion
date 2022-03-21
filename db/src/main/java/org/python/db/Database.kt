package org.python.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.python.db.daos.AnniversaryDao
import org.python.db.daos.NoteCategoryDao
import org.python.db.daos.NoteDao
import org.python.db.entities.RoomAnniversary
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.typeconverters.ColorConverter
import org.python.db.typeconverters.DurationConverter
import org.python.db.typeconverters.InstantConverter

@Database(entities = [RoomNote::class, RoomNoteCategory::class, RoomAnniversary::class], version = 1)
@TypeConverters(ColorConverter::class, DurationConverter::class, InstantConverter::class)
abstract class CompanionDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val noteCategoryDao: NoteCategoryDao
    abstract val anniversaryDao: AnniversaryDao

    companion object {
        @Volatile
        private var INSTANCE: CompanionDatabase? = null
        private const val DB_NAME: String = "companion_note.db"

        fun getInstance(context: Context): CompanionDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            CompanionDatabase::class.java,
                            DB_NAME
                        ).build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}