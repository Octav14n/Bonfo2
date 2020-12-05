package eu.schnuff.bonfo2.data.historyItem

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import java.util.*

@Entity(tableName = "historyitem")
@ForeignKey(
    entity = EPubItem::class,
    parentColumns = ["item"],
    childColumns = ["url"]
)
data class HistoryItem (
    val item: String,
    val action: ACTION,
    val time: Date = Date()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}

enum class ACTION(val value: Int) {
    VIEW(1)
}