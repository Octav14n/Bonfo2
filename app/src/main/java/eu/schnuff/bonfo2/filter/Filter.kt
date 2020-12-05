package eu.schnuff.bonfo2.filter

import android.widget.TextView
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.helper.setHighlightedText
import java.util.Locale

class Filter {
    var searchString: String = ""
        set(value) {
            if (value == searchString) return
            field = value
            search = searchFilterFrom(value)
            notifyChangeListener()
        }
    internal var search: SearchFilter = EmptySearchFilter; private set
    private val onChangeListener = mutableListOf<(it: Filter) -> Unit>()
    var excludeGenres: Set<String> = emptySet()
        set(value) {
            val v = value.map { it.toLowerCase(Locale.getDefault()) }.toSet()
            if (field == v)
                return
            field = v
            notifyChangeListener()
        }
    var minFileSize: Int = -1
        set(value) {
            if (field == value)
                return
            field = value
            notifyChangeListener()
        }

    fun apply(items: List<EPubItem>): List<EPubItem> {
        return items.filter {
            (minFileSize == -1 || it.fileSize >= minFileSize) &&
            (excludeGenres.isEmpty() || !it.genres.any { it.toLowerCase(Locale.getDefault()) in excludeGenres }) &&
            search.applies(it)
        }
    }

    fun addChangeListener(listener: (it: Filter) -> Unit) {
        onChangeListener.add(listener)
    }

    private fun notifyChangeListener() {
        onChangeListener.forEach { it(this) }
    }

    companion object {
        private const val FILTER_MIN_LENGTH = 3
        private const val FILTER_SPLITTER = ' '
        private const val FILTER_REPLACER = '_'

        private fun searchFilterFrom(s: String, minStringLength: Int = FILTER_MIN_LENGTH): SearchFilter {
            return when {
                s.isEmpty() -> EmptySearchFilter
                ' ' in s -> AllSearchFilter(
                        s.split(FILTER_SPLITTER)
                            .filter { it.length >= minStringLength }
                            .map { searchFilterFrom(it, minStringLength) }
                    )
                else -> StringSearchFilter(s.replace(FILTER_REPLACER, FILTER_SPLITTER))
            }
        }
    }
}

fun TextView.setHighlightedText(s: String?, filter: Filter, HIGHLIGHT_COLORS: Array<Int>, formattedBy: Int? = null) {
    this.setHighlightedText(s, filter.search.getStrings(), HIGHLIGHT_COLORS, formattedBy)
}
fun TextView.setHighlightedText(s: Array<String>, filter: Filter, HIGHLIGHT_COLORS: Array<Int>, formattedBy: Int? = null) {
    this.setHighlightedText(s, filter.search.getStrings(), HIGHLIGHT_COLORS, formattedBy)
}