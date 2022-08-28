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
import java.util.*
import kotlin.concurrent.thread

class BookAdapter(
    private val onClickListener: (item: EPubItem) -> Unit = {},
    private val onLongClickListener: (item: EPubItem) -> Unit = {},
    private val onListChanged: (previousList: MutableList<EPubItem>, currentList: MutableList<EPubItem>) -> Unit = {_,_->}
) : ListAdapter<EPubItem, BookItem>(DIFF) {
    val filter = Filter()
    private var originalList: List<EPubItem> = mutableListOf()
    var lastOpened : List<String> = listOf()
        set(value) {
            if (value == field) return
            field = value
            BookItem.LastOpened = value

            if (sortBy == SortBy.ACCESS)
                sort(null)
        }
    lateinit var sortBy: SortBy
    lateinit var sortOrder: SortOrder
    private var refreshJob: Thread? = null

    init {
        filter.addChangeListener {
            refresh(reason = RefreshReason.CHANGE_FILTER)
        }
        BookItem.Filter = this.filter
    }

    override fun submitList(list: List<EPubItem>?) {
        if (list == null)
            return
        originalList = sort(list)
        refresh(
            reason = RefreshReason.NEW_LIST
        )
    }

    override fun onCurrentListChanged(previousList: MutableList<EPubItem>, currentList: MutableList<EPubItem>) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isNotEmpty())
            onListChanged(previousList, currentList)
    }

    private fun refresh(
        appliedList: List<EPubItem> = currentList,
        reason: RefreshReason = RefreshReason.NEW_LIST
    ) {
        //refreshJob?.stop()
        refreshJob = thread {
            var list: List<EPubItem> = if (reason == RefreshReason.NEW_LIST || reason == RefreshReason.CHANGE_FILTER) filter.apply(originalList) else appliedList
            if (reason == RefreshReason.NEW_LIST || reason == RefreshReason.CHANGE_SORT) {
                list = sort(list)
                if (list !== currentList)
                    if (reason == RefreshReason.CHANGE_SORT && currentList.size > 100)
                        super.submitList(emptyList()) { super.submitList(list) }
                    else
                        super.submitList(list)
            } else if (list !== currentList)
                super.submitList(list)
        }
    }

    fun filter(filter: String) {
        this.filter.searchString = filter
    }

    fun setSort(by: SortBy, order: SortOrder) {
        sortBy = by
        sortOrder = order
        originalList = sort(null)
        refresh(reason = RefreshReason.CHANGE_SORT)
    }
    private fun sort(list: List<EPubItem>?): List<EPubItem> {
        val comparator = when (sortBy) {
            SortBy.ACCESS -> compareByDescending  { val i = lastOpened.indexOf(it.url); if (i==-1) Integer.MAX_VALUE else i }
            SortBy.CREATION -> compareBy(EPubItem::modified)
            SortBy.SIZE -> compareBy(EPubItem::fileSize)
        }.run {
            if (sortOrder == SortOrder.DESC)
                Collections.reverseOrder(this)
            else
                this
        }.then(compareBy(EPubItem::modified))

        return (list ?: originalList).sortedWith(comparator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookItem {
        val bookItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_epub, parent, false) as View
        return BookItem(
            bookItemView,
            onClickListener = { onClickListener(getItem(it)) },
            onLongClickListener = { onLongClickListener(getItem(it)) },
        )
    }

    override fun onBindViewHolder(holder: BookItem, position: Int) {
        holder.bindTo(getItem(position))
    }

    private enum class RefreshReason {
        NEW_LIST,
        CHANGE_SORT,
        CHANGE_FILTER
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EPubItem>() {
            override fun areItemsTheSame(oldItem: EPubItem, newItem: EPubItem): Boolean {
                return oldItem.filePathHash == newItem.filePathHash
            }

            override fun areContentsTheSame(oldItem: EPubItem, newItem: EPubItem): Boolean {
                return oldItem == newItem
            }

        }
    }
}