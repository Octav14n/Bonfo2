package eu.schnuff.bonfo2.data.ePubItem

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface EPubItemDAO {
    @Query("SELECT * FROM epubitem ORDER BY modified DESC, author, url")
    fun getAll(): LiveData<List<EPubItem>>

    @Query("SELECT * FROM epubitem ORDER BY modified DESC, author, url")
    fun getAllNow(): List<EPubItem>

    @Update
    fun update(ePubItem: EPubItem)

    @Insert
    fun insert(vararg ePubItem: EPubItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(vararg ePubItem: EPubItem)

    @Delete
    fun delete(ePubItem: EPubItem)

    @Delete
    fun delete(ePubItems: Collection<EPubItem>)
}