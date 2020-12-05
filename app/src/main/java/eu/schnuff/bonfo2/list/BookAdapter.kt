package eu.schnuff.bonfo2.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.filter.Filter

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
    var filterSmall
    get() = filter.minFileSize != -1
    set(value) {
        filter.minFileSize = if (value) 120 * 1024 * 1024 else -1
    }

    init {
        filter.addChangeListener {
            refresh(updateFiltered = true)
        }
        BookItem.Filter = this.filter
    }

    override fun submitList(list: List<EPubItem>?) {
        if (list == null)
            return
        originalList = list
        refresh(
            updateFiltered = true
        )
    }

    override fun onCurrentListChanged(previousList: MutableList<EPubItem>, currentList: MutableList<EPubItem>) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isNotEmpty())
            onListChanged(previousList, currentList)
    }

    private fun refresh(
        appliedList: List<EPubItem> = currentList,
        updateFiltered: Boolean = false
    ) {
        val list: List<EPubItem> = if (updateFiltered) filter.apply(originalList) else appliedList

        if (list !== currentList)
            super.submitList(list)
    }

    fun filter(filter: String) {
        this.filter.searchString = filter
    }

    fun sort(by: SortBy) {
        originalList = originalList.sortedBy {
            when (by) {
                SortBy.ACCESS -> TODO()
                SortBy.CREATION -> it.modified
            }
        }
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

    enum class SortBy {
        ACCESS,
        CREATION
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