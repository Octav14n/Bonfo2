package eu.schnuff.bonfo2.helper

import android.content.Context
import androidx.preference.PreferenceManager

private const val PREFERENCE_WATCHED_DIRECTORIES = "watched_directories"
private const val PREFERENCE_LIST_SCROLL_IDX = "list_first_item_idx"
private const val PREFERENCE_LIST_FILTER = "list_filter"
private const val PREFERENCE_SORT_ORDER = "sort_order"
private const val PREFERENCE_SORT_BY_FILTER = "sort_by_filter"
private const val PREFERENCE_SHOW_SMALL_FILTER = "show_small_filter"
private const val PREFERENCE_SHOW_NSFW_FILTER = "show_nsfw_filter"
private const val PREFERENCE_MIN_FILE_SIZE = "min_file_size"

class Setting(context: Context, onChange: (it: Setting) -> Unit = {}) {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        registerOnChangeListener(onChange)
    }

    var watchedDirectories: Set<String>
        get() = pref.getStringSet(PREFERENCE_WATCHED_DIRECTORIES, emptySet()) ?: setOf()
        set(value) = pref.edit().putStringSet(PREFERENCE_WATCHED_DIRECTORIES, value).apply()

    fun addWatchedDirectory(directory: String) {
        watchedDirectories = watchedDirectories + directory
    }

    var listScrollIdx: Int
        get() = pref.getInt(PREFERENCE_LIST_SCROLL_IDX, 0)
        set(value) = pref.edit().putInt(PREFERENCE_LIST_SCROLL_IDX, value).apply()

    var filter: String
        get() = pref.getString(PREFERENCE_LIST_FILTER, "") ?: ""
        set(value) = pref.edit().putString(PREFERENCE_LIST_FILTER, value).apply()

    var sortBy: SortBy
        get() = SortBy.valueOf(pref.getString(PREFERENCE_SORT_BY_FILTER, SortBy.CREATION.name) ?: SortBy.CREATION.name)
        set(value) = pref.edit().putString(PREFERENCE_SORT_BY_FILTER, value.name).apply()

    var sortOrder: SortOrder
        get() = SortOrder.valueOf(pref.getString(PREFERENCE_SORT_ORDER, SortOrder.DESC.name) ?: SortOrder.DESC.name)
        set(value) = pref.edit().putString(PREFERENCE_SORT_ORDER, value.name).apply()

    var showSmall: Boolean
        get() = pref.getBoolean(PREFERENCE_SHOW_SMALL_FILTER, true)
        set(value) = pref.edit().putBoolean(PREFERENCE_SHOW_SMALL_FILTER, value).apply()

    var showNsfw: Boolean
        get() = pref.getBoolean(PREFERENCE_SHOW_NSFW_FILTER, true)
        set(value) = pref.edit().putBoolean(PREFERENCE_SHOW_NSFW_FILTER, value).apply()

    var minFileSize: Int
        get() = pref.getString(PREFERENCE_MIN_FILE_SIZE, "120")!!.toInt() * 1024
        set(value) = pref.edit().putString(PREFERENCE_MIN_FILE_SIZE, (value / 1024).toString()).apply()

    fun registerOnChangeListener(onChange: (it: Setting) -> Unit) {
        pref.registerOnSharedPreferenceChangeListener { _, _ ->
            onChange(this)
        }
    }
}