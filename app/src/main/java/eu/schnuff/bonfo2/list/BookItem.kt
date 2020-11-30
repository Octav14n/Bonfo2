package eu.schnuff.bonfo2.list

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.helper.setHighlightedText
import eu.schnuff.bonfo2.databinding.ListEpubBinding


const val HIGHLIGHT = "<font color='%s'>$1</font>"
const val STROKE_WIDTH = 8

class BookItem(private val containerView: View, private val onClickListener: (itemIdx: Int) -> Unit = {}) :
        RecyclerView.ViewHolder(containerView),
        View.OnClickListener
{
    private val binding = ListEpubBinding.bind(containerView)
    private val background = binding.linearLayout.background as GradientDrawable
    private var boundTo: EPubItem? = null
        set(value) {
            field = value
            redraw(filter = true, opened = true)
        }

    private fun redraw(filter: Boolean = false, opened: Boolean = false) {
        val value = this.boundTo ?: return

        if (opened) {
            val idx = LastOpened.indexOf(value.url)
            if (idx == -1)
                background.setStroke(STROKE_WIDTH, STROKE_COLOR[0])
            else
                background.setStroke(
                    STROKE_WIDTH,
                    STROKE_COLOR[((idx.toFloat() / LastOpened.size) * STROKE_COLOR.size).toInt() + 1]
                )
        }
        if (filter) {
            binding.listTitle.setHighlightedText(value.title, Filter, HIGHLIGHT_COLOR)
            binding.listAuthor.setHighlightedText(value.author, Filter, HIGHLIGHT_COLOR, R.string.list_author)
            binding.listDescription.setHighlightedText(value.description, Filter, HIGHLIGHT_COLOR)
            binding.listSubjects.setHighlightedText(
                value.genres.union(value.characters.toList()).toTypedArray(),
                Filter,
                HIGHLIGHT_COLOR
            )
            binding.listSize.text = value.size
        }
    }

    init {
        instances.add(this)
        containerView.setOnClickListener(this)
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

    fun bindTo(item: EPubItem) {
        boundTo = item
    }

    override fun onClick(v: View?) {
        onClickListener(adapterPosition)
    }

    companion object {
        private val instances = mutableListOf<BookItem>()
        var Filter: Collection<String> = listOf()
            set(value) {
                field = value
                instances.forEach { it.redraw(filter = true) }
            }
        var LastOpened: Collection<String> = listOf()
            set(value) {
                field = value
                instances.forEach { it.redraw(opened = true) }
            }
        var parentActivity: Activity? = null
        private var HIGHLIGHT_COLOR = emptyArray<Int>()
        private var STROKE_COLOR = emptyArray<Int>()
    }
}