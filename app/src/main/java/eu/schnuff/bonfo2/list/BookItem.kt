package eu.schnuff.bonfo2.list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.databinding.ListEpubBinding
import eu.schnuff.bonfo2.filter.Filter
import eu.schnuff.bonfo2.filter.setHighlightedText
import kotlin.math.min


const val HIGHLIGHT = "<font color='%s'>%s</font>"
const val STROKE_WIDTH = 8
const val LIST_MAX_SIZE = 25

class BookItem(
    containerView: View,
    private val onClickListener: (itemIdx: Int) -> Unit = {},
    private val onLongClickListener: (itemIdx: Int) -> Unit = {}
) :
        RecyclerView.ViewHolder(containerView),
        View.OnClickListener,
        View.OnLongClickListener
{
    private val binding = ListEpubBinding.bind(containerView)
    private var boundTo: EPubItem? = null
        set(value) {
            field = value
            redraw(filter = true, opened = true)
        }

    private fun redraw(filter: Boolean = false, opened: Boolean = false) {
        val value = this.boundTo
        if (value == null) {
            binding.bookCard.cardElevation = 0f
            binding.listPlaceholder.visibility = View.VISIBLE
            binding.listLayout.visibility = View.GONE
            return
        }

        binding.listPlaceholder.visibility = View.GONE
        binding.listLayout.visibility = View.VISIBLE

        if (opened) {
            val idx = LastOpened.indexOf(value.url)

            if (idx == -1) {
                binding.bookCard.cardElevation = 0f
                //binding.bookCard.strokeWidth = 0
                //binding.bookCard.strokeColor = STROKE_COLOR[0]
            } else {
                binding.bookCard.cardElevation = ((LIST_MAX_SIZE - idx).toFloat() / LIST_MAX_SIZE) * binding.bookCard.maxCardElevation
                //binding.bookCard.strokeWidth = STROKE_WIDTH
                //binding.bookCard.strokeColor =
                //    STROKE_COLOR[((idx.toFloat() / LastOpened.size) * STROKE_COLOR.size).toInt() + 1]
            }
        }
        if (filter) {
            binding.listTitle.setHighlightedText(value.title, Filter!!, HIGHLIGHT_COLOR)
            binding.listAuthor.setHighlightedText(value.author, Filter!!, HIGHLIGHT_COLOR, R.string.list_author)
            binding.listDescription.setHighlightedText(value.description, Filter!!, HIGHLIGHT_COLOR)
            binding.listSubjects.setHighlightedText(
                value.genres.union(value.characters.toList()).toTypedArray(),
                Filter!!,
                HIGHLIGHT_COLOR
            )
            binding.listSize.text = value.size
        }
    }

    init {
        instances.add(this)
        containerView.setOnClickListener(this)
        containerView.setOnLongClickListener(this)
        if (HIGHLIGHT_COLOR.isEmpty()) {
            val colors = containerView.context.resources.obtainTypedArray(R.array.filter_highlights)
            HIGHLIGHT_COLOR = Array(colors.length()) { colors.getColor(it, 0) }
            assert(HIGHLIGHT_COLOR.isNotEmpty())
            colors.recycle()
        }
        if (STROKE_COLOR.isEmpty()) {
            val colors = containerView.context.resources.obtainTypedArray(R.array.list_strokes)
            STROKE_COLOR = Array(colors.length()) { colors.getColor(it, 0) }
            assert(STROKE_COLOR.isNotEmpty())
            colors.recycle()
        }
    }

    fun bindTo(item: EPubItem?) {
        boundTo = item
    }

    override fun onClick(v: View?) {
        onClickListener(adapterPosition)
    }

    override fun onLongClick(v: View?): Boolean {
        onLongClickListener(adapterPosition)
        return true
    }

    companion object {
        private val instances = mutableListOf<BookItem>()
        internal var Filter: Filter? = null
            set(value) {
                field = value
                value?.addChangeListener {
                    instances.forEach { it.redraw(filter = true) }
                }
            }
        var LastOpened: List<String> = listOf()
            set(value) {
                field = value.subList(0, min(LIST_MAX_SIZE, value.size))
                instances.forEach { it.redraw(opened = true) }
            }
        private var HIGHLIGHT_COLOR = emptyArray<Int>()
        private var STROKE_COLOR = emptyArray<Int>()
    }
}