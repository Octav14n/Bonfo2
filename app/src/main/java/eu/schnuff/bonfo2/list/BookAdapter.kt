package eu.schnuff.bonfo2.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.filter.Filter
import eu.schnuff.bonfo2.helper.SortBy
import eu.schnuff.bonfo2.helper.SortOrder

class BookAdapter(
    private val onClickListener: (item: EPubItem) -> Unit = {},
    private val onListChanged: (previousList: MutableList<EPubItem>, currentList: MutableList<EPubItem>) -> Unit = {_,_->}
) : ListAdapter<EPubItem, BookItem>(DIFF) {
    val filter = Filter()
    private var originalList: List<EPubItem> = mutableListOf()
    var lastOpened : Collection<String> = listOf()
        set(value) {
            if (value == field) return
            field = value
            BookItem.LastOpened = value
        }
    lateinit var sortBy: SortBy
    lateinit var sortOrder: SortOrder

    init {
        filter.addChangeListener {
            refresh(updateFiltered = true)
        }
        BookItem.Filter = this.filter
    }

    override fun submitList(list: List<EPubItem>?) {
        if (list == null)
            return
        originalList = sort(list)
        refresh(
            updateFiltered = true,
            updateSort = true
        )
    }

    override fun onCurrentListChanged(previousList: MutableList<EPubItem>, currentList: MutableList<EPubItem>) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isNotEmpty())
            onListChanged(previousList, currentList)
    }

    private fun refresh(
        appliedList: List<EPubItem> = currentList,
        updateFiltered: Boolean = false,
        updateSort: Boolean = false
    ) {
        var list: List<EPubItem> = if (updateFiltered) filter.apply(originalList) else appliedList
        if (updateSort)
            list = sort(list)

        if (list !== currentList)
            super.submitList(list)
    }

    fun filter(filter: String) {
        this.filter.searchString = filter
    }

    fun setSort(by: SortBy, order: SortOrder) {
        sortBy = by
        sortOrder = order
        originalList = sort(null)
        refresh(updateSort = true)
    }
    private fun sort(list: List<EPubItem>?): List<EPubItem> {
        val newlist = (list ?: originalList).toMutableList()
        when (sortBy) {
            SortBy.ACCESS -> newlist.sortBy { lastOpened.indexOf(it.filePath) }
            SortBy.CREATION -> newlist.sortWith (compareBy(EPubItem::modified))
        }
        if (sortOrder == SortOrder.DESC)
            newlist.reverse()
        return newlist
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookItem {
        val bookItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_epub, parent, false) as View
        return BookItem(bookItemView) {
            onClickListener(getItem(it))
        }
    }

    override fun onBindViewHolder(holder: BookItem, position: Int) {
        holder.bindTo(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EPubItem>() {
            override fun areItemsTheSame(oldItem: EPubItem, newItem: EPubItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: EPubItem, newItem: EPubItem): Boolean {
                return oldItem == newItem
            }

        }
    }
}