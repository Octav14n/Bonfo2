package eu.schnuff.bonfo2.filter

import eu.schnuff.bonfo2.data.ePubItem.EPubItem

internal interface SearchFilter {
    fun applies(item: EPubItem): Boolean
    fun getRegexes(): Set<Regex>
}

internal object EmptySearchFilter: SearchFilter {
    override fun applies(item: EPubItem) = true
    override fun getRegexes() = emptySet<Regex>()
}

internal class StringSearchFilter(private val s: String) : SearchFilter {
    private val r = try { s.toRegex(RegexOption.IGNORE_CASE) } catch (_: java.lang.Exception) { s.toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.LITERAL)) }
    override fun applies(item: EPubItem) = item.contains(r)
    override fun getRegexes() = setOf(r)
}

internal class AllSearchFilter(private val filters: List<SearchFilter>) : SearchFilter {
    override fun applies(item: EPubItem) = filters.all { it.applies(item) }
    override fun getRegexes() = filters.flatMap { it.getRegexes() }.toSet()
}
