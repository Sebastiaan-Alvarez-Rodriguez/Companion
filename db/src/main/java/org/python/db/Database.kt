package org.python.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import org.python.db.daos.AnniversaryDao
import org.python.db.daos.NoteCategoryDao
import org.python.db.daos.NoteDao
import org.python.db.entities.RoomAnniversary
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.typeconverters.ColorConverter
import org.python.db.typeconverters.DurationConverter
import org.python.db.typeconverters.InstantConverter

@Database(entities = [RoomNote::class, RoomNoteCategory::class, RoomAnniversary::class], version = BuildConfig.DB_VERSION)
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
                        ).addCallback(object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                buildDb(db)
                            }
                        }).build()
                    }
                }
            }
            return INSTANCE!!
        }

        private fun buildDb(db: SupportSQLiteDatabase) {
            db.execSQL("BEGIN TRANSACTION;")
            db.execSQL("INSERT OR IGNORE INTO RoomNoteCategory VALUES(${noteCategorySQLValueBuilder(RoomNoteCategory.DEFAULT)});")
            db.execSQL("COMMIT;")
        }

        private fun noteCategorySQLValueBuilder(category: RoomNoteCategory): String {
            return "${category.categoryId}, '${category.categoryName}', ${ColorConverter.colorToLong(category.color)}, ${if (category.favorite) 1 else 0}, ${InstantConverter.dateToTimestamp(category.categoryDate)}"
        }
    }
}