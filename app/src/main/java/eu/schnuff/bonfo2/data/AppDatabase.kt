package eu.schnuff.bonfo2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.data.ePubItem.EPubItemDAO
import eu.schnuff.bonfo2.data.historyItem.HistoryItem
import eu.schnuff.bonfo2.data.historyItem.HistoryItemDAO

@Database(entities = [EPubItem::class, HistoryItem::class], version = 2, exportSchema = false)
@TypeConverters(PersistenceConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ePubItemDao() : EPubItemDAO
    abstract fun historyItemDao() : HistoryItemDAO

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "word_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}