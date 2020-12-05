package eu.schnuff.bonfo2.filter

import eu.schnuff.bonfo2.data.ePubItem.EPubItem

internal interface SearchFilter {
    fun applies(item: EPubItem): Boolean
    fun getStrings(): Set<String>
}

internal object EmptySearchFilter: SearchFilter {
    override fun applies(item: EPubItem) = true
    override fun getStrings() = emptySet<String>()
}

internal class StringSearchFilter(private val s: String) : SearchFilter {
    override fun applies(item: EPubItem) = item.contains(s)
    override fun getStrings() = setOf(s)
}

internal class AllSearchFilter(private val filters: List<SearchFilter>) : SearchFilter {
    override fun applies(item: EPubItem) = filters.all { it.applies(item) }
    override fun getStrings() = filters.flatMap { it.getStrings() }.toSet()
}
