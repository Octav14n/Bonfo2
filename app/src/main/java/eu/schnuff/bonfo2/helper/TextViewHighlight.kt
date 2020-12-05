package eu.schnuff.bonfo2.helper

import android.view.View
import android.widget.TextView
import androidx.core.text.HtmlCompat
import eu.schnuff.bonfo2.list.HIGHLIGHT


fun TextView.setHighlightedText(text: String?, highlighting: Collection<String>, HIGHLIGHT_COLORS: Array<Int>, formattedBy: Int? = null) {
    if (text.isNullOrEmpty()) {
        this.visibility = View.GONE
    } else {
        this.visibility = View.VISIBLE
        val value = highlight(text, highlighting, HIGHLIGHT_COLORS)
        val formatted = if (formattedBy == null) value else context.getString(formattedBy, value)
        val htmlified = formatted.replace("\n", "<br>")
        this.text = HtmlCompat.fromHtml(htmlified, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}
fun TextView.setHighlightedText(texts: Array<String>, highlighting: Collection<String>, HIGHLIGHT_COLORS: Array<Int>, formattedBy: Int? = null) {
    if (texts.isEmpty()) {
        this.visibility = View.GONE
    } else {
        this.visibility = View.VISIBLE
        val value = highlight(texts, highlighting, HIGHLIGHT_COLORS)
        val formatted = if (formattedBy == null) value else context.getString(formattedBy, value)
        val htmlified = formatted.replace("\n", "<br>")
        this.text = HtmlCompat.fromHtml(htmlified, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}
private fun highlight(item: String?, highlighting: Collection<String>, HIGHLIGHT_COLORS: Array<Int>) : String {
    return when {
        item === null -> ""
        highlighting.isNotEmpty() -> {
            highlighting.foldIndexed(item) { myI, acc, highlight ->
                val i = myI % HIGHLIGHT_COLORS.size
                acc.replace("($highlight)".toRegex(RegexOption.IGNORE_CASE), HIGHLIGHT.format(HIGHLIGHT_COLORS[i]))
            }
        }
        else -> item
    }
}
private fun highlight(items: Array<String>, highlighting: Collection<String>, HIGHLIGHT_COLORS: Array<Int>) : String =
    items.joinToString { s -> highlight(s, highlighting, HIGHLIGHT_COLORS) }