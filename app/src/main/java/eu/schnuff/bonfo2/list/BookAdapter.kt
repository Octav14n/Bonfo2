package eu.schnuff.bonfo2.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.AppDatabase
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.data.ePubItem.EPubItemDAO
import eu.schnuff.bonfo2.filter.Filter
import eu.schnuff.bonfo2.helper.SortBy
import eu.schnuff.bonfo2.helper.SortOrder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

class BookAdapter(
    private val onClickListener: (item: EPubItem) -> Unit = {},
    private val onLongClickListener: (item: EPubItem) -> Unit = {},
    private val onListChanged: (adapter: BookAdapter, currentList: PagingData<EPubItem>) -> Unit = {_,_->}
) : PagingDataAdapter<EPubItem, BookItem>(DIFF) {
    val filter = Filter()
    private var originalList: PagingData<EPubItem> = PagingData.empty()
    var lastOpened : List<String> = listOf()
        set(value) {
            if (value == field) return
            field = value
            BookItem.LastOpened = value

            if (sortBy == SortBy.ACCESS)
                thread { runBlocking { refresh(null) } }
        }
    private var sortBy = SortBy.CREATION
    private var sortOrder = SortOrder.DESC
    private lateinit var dao: EPubItemDAO

    init {
        filter.addChangeListener {
            thread { runBlocking {
                refresh(originalList)
            } }
        }
        BookItem.Filter = this.filter
    }

    fun init(context: Context) {
        dao = AppDatabase.getDatabase(context).ePubItemDao()
    }

    suspend fun fetchData() {
        coroutineScope {
            Pager(
                PagingConfig(
                    initialLoadSize = 20,
                    pageSize = 1000,
                    prefetchDistance = 100000,
                )
            ) {
                dao.getAll(sortBy, sortOrder)
            }.flow.cachedIn(this).collectLatest {
                refresh(it)
            }
        }
    }

    private suspend fun refresh(
        appliedList: PagingData<EPubItem>?
    ) {
        val list = filter.apply(originalList)
        if (appliedList != null) {
            originalList = appliedList
            onListChanged(this, list)
        }
        super.submitData(list)
    }

    fun filter(filter: String) {
        this.filter.searchString = filter
    }

    suspend fun setSort(by: SortBy, order: SortOrder) {
        sortBy = by
        sortOrder = order
        fetchData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookItem {
        val bookItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_epub, parent, false) as View
        return BookItem(
            bookItemView,
            onClickListener = { onClickListener(getItem(it)!!) },
            onLongClickListener = { onLongClickListener(getItem(it)!!) },
        )
    }

    override fun onBindViewHolder(holder: BookItem, position: Int) {
        holder.bindTo(getItem(position))
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