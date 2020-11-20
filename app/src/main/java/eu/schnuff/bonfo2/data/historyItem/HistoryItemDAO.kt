package eu.schnuff.bonfo2.data.historyItem

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryItemDAO {
    @Query("SELECT * FROM historyitem ORDER BY time DESC")
    fun getAll(): LiveData<List<HistoryItem>>

    @Insert
    fun insert(item: HistoryItem)
}