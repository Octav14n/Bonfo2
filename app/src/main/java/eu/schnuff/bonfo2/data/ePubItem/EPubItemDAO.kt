package eu.schnuff.bonfo2.data.ePubItem

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import eu.schnuff.bonfo2.helper.SortBy
import eu.schnuff.bonfo2.helper.SortOrder

@Dao
interface EPubItemDAO {
    @Query("SELECT * FROM epubitem ORDER BY modified DESC, author, url")
    fun getAll(): PagingSource<Int, EPubItem>

    @RawQuery(observedEntities = [EPubItem::class])
    fun getAll(query: SupportSQLiteQuery): PagingSource<Int, EPubItem>

    @Query("SELECT * FROM epubitem ORDER BY "
        + "CASE WHEN :sortBy = 'modified' AND :sortOrder = 0 THEN modified END DESC, "
        + "CASE WHEN :sortBy = 'modified' AND :sortOrder = 1 THEN modified END ASC, "
        + "CASE WHEN :sortBy = 'fileSize' AND :sortOrder = 0 THEN fileSize END DESC, "
        + "CASE WHEN :sortBy = 'fileSize' AND :sortOrder = 1 THEN fileSize END ASC "
    )
    fun getAll(sortBy: String, sortOrder: Boolean): PagingSource<Int, EPubItem>

    fun getAll(sortBy: SortBy, sortOrder: SortOrder): PagingSource<Int, EPubItem> {
        val by = when (sortBy) {
            SortBy.CREATION -> "modified"
            SortBy.ACCESS -> "modified"
            SortBy.SIZE -> "fileSize"
        }
        return getAll(by, sortOrder == SortOrder.ASC)
    }

    @Query("SELECT * FROM epubitem WHERE fileSize > :size  ORDER BY modified DESC, author, url")
    fun getFiltered(size: Int): LiveData<List<EPubItem>>

    @Query("SELECT * FROM epubitem ORDER BY modified DESC, author, url")
    fun getAllNow(): List<EPubItem>

    @Query("SELECT MAX(modified) FROM epubitem")
    fun getLastModified(): Long

    @Update
    fun update(ePubItem: EPubItem)

    @Insert
    fun insert(vararg ePubItem: EPubItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(vararg ePubItem: EPubItem)

    @Query("DELETE FROM epubitem WHERE filePath = :filePath")
    fun delete(filePath: String)

    @Delete
    fun delete(ePubItem: EPubItem)

    @Delete
    fun delete(ePubItems: Collection<EPubItem>)

    @Query("DELETE FROM epubitem")
    fun devDeleteAll()
}