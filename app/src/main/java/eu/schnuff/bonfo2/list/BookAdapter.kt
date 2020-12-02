package eu.schnuff.bonfo2.list

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.ePubItem.EPubItem

private const val FILTER_MIN_LENGTH = 3

class BookAdapter(
    private val onClickListener: (item: EPubItem) -> Unit = {},
    private val onListChanged: (previousList: MutableList<EPubItem>, currentList: MutableList<EPubItem>) -> Unit = {_,_->}
) : ListAdapter<EPubItem, BookItem>(DIFF) {
    private var filter = ""
    private val filters: List<String>
        get() = if (filter.length < FILTER_MIN_LENGTH) emptyList() else {
            filter
                .split(" ")
                .map { it.replace('-', ' ') }
                .filter { it.length >= FILTER_MIN_LENGTH }
        }
    private var originalList: List<EPubItem> = mutableListOf()
    var lastOpened : Collection<String> = listOf()
        set(value) {
            if (value == field) return
            field = value
            BookItem.LastOpened = value
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
        val list: List<EPubItem> = if (updateFiltered) {
            if (filter.isEmpty() || filter.length < FILTER_MIN_LENGTH) {
                originalList
            } else {
                originalList.filter { item -> filters.all { filter -> item.contains(filter) } }
            }
        } else appliedList

        if (list !== currentList)
            super.submitList(list)
    }

    fun filter(filter: String) {
        this.filter = filter
        BookItem.Filter = filters
        refresh(updateFiltered = true)
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