package org.python.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.python.db.daos.AnniversaryDao
import org.python.db.daos.NoteDao
import org.python.db.entities.RoomAnniversary
import org.python.db.entities.RoomNote
import org.python.db.typeconverters.DurationConverter
import org.python.db.typeconverters.InstantConverter

@Database(entities = [RoomNote::class, RoomAnniversary::class], version = 1)
@TypeConverters(DurationConverter::class, InstantConverter::class)
abstract class CompanionDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
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