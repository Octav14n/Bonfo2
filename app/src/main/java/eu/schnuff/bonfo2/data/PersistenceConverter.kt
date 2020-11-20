package eu.schnuff.bonfo2.data

import android.os.Build
import androidx.room.TypeConverter
import eu.schnuff.bonfo2.data.historyItem.ACTION
import org.json.JSONArray
import java.util.*
import kotlin.collections.ArrayList

class PersistenceConverter {
    @TypeConverter
    fun fromStringArray(array: Array<String>): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            JSONArray(array).toString()
        } else {
            val c = ArrayList<String>()
            array.toCollection(c)
            JSONArray(c).toString()
        }
    }

    @TypeConverter
    fun toStringArray(string: String): Array<String> {
        val array = JSONArray(string)
        return Array(array.length()) {
            array[it] as String
        }
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(long: Long): Date {
        return Date(long)
    }

    @TypeConverter
    fun fromHistoryEnum(item: ACTION) = item.value
    @TypeConverter
    fun toHistoryEnum(item: Int) = ACTION.values()[item - 1]
}