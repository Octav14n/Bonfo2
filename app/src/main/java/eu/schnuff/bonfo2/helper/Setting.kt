package eu.schnuff.bonfo2.helper

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

enum class PREFERENCE(val string: String) {
    PREFERENCE_WATCHED_DIRECTORIES	("watched_directories"),
    PREFERENCE_LIST_SCROLL_IDX	    ("list_first_item_idx"),
    PREFERENCE_LIST_FILTER      	("list_filter"),
    PREFERENCE_SORT_ORDER	        ("sort_order"),
    PREFERENCE_SORT_BY_FILTER	    ("sort_by_filter"),
    PREFERENCE_SHOW_SMALL_FILTER	("show_small_filter"),
    PREFERENCE_SHOW_NSFW_FILTER 	("show_nsfw_filter"),
    PREFERENCE_MIN_FILE_SIZE	    ("min_file_size"),
    PREFERENCE_USE_MEDIASTORE       ("developer_use_mediastore"),
    INTERNAL_LAST_MODIFIED          ("last_modified"),
}

class Setting(context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val pref = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val changeListeners = mutableMapOf<String, (Setting) -> Unit>()

    init {
        pref.registerOnSharedPreferenceChangeListener(this)
    }

    var watchedDirectories: Set<String>
        get() = pref.getStringSet(PREFERENCE.PREFERENCE_WATCHED_DIRECTORIES.string, emptySet()) ?: setOf()
        set(value) = pref.edit().putStringSet(PREFERENCE.PREFERENCE_WATCHED_DIRECTORIES.string, value).apply()

    fun addWatchedDirectory(directory: String) {
        watchedDirectories = watchedDirectories + directory
    }

    var listScrollIdx: Int
        get() = pref.getInt(PREFERENCE.PREFERENCE_LIST_SCROLL_IDX.string, 0)
        set(value) = pref.edit().putInt(PREFERENCE.PREFERENCE_LIST_SCROLL_IDX.string, value).apply()

    var filter: String
        get() = pref.getString(PREFERENCE.PREFERENCE_LIST_FILTER.string, "") ?: ""
        set(value) = pref.edit().putString(PREFERENCE.PREFERENCE_LIST_FILTER.string, value).apply()

    var sortBy: SortBy
        get() = SortBy.valueOf(pref.getString(PREFERENCE.PREFERENCE_SORT_BY_FILTER.string, SortBy.CREATION.name) ?: SortBy.CREATION.name)
        set(value) = pref.edit().putString(PREFERENCE.PREFERENCE_SORT_BY_FILTER.string, value.name).apply()

    var sortOrder: SortOrder
        get() = SortOrder.valueOf(pref.getString(PREFERENCE.PREFERENCE_SORT_ORDER.string, SortOrder.DESC.name) ?: SortOrder.DESC.name)
        set(value) = pref.edit().putString(PREFERENCE.PREFERENCE_SORT_ORDER.string, value.name).apply()

    var showSmall: Boolean
        get() = pref.getBoolean(PREFERENCE.PREFERENCE_SHOW_SMALL_FILTER.string, true)
        set(value) = pref.edit().putBoolean(PREFERENCE.PREFERENCE_SHOW_SMALL_FILTER.string, value).apply()

    var showNsfw: Boolean
        get() = pref.getBoolean(PREFERENCE.PREFERENCE_SHOW_NSFW_FILTER.string, true)
        set(value) = pref.edit().putBoolean(PREFERENCE.PREFERENCE_SHOW_NSFW_FILTER.string, value).apply()

    var minFileSize: Int
        get() = pref.getString(PREFERENCE.PREFERENCE_MIN_FILE_SIZE.string, "120")!!.toInt() * 1024
        set(value) = pref.edit().putString(PREFERENCE.PREFERENCE_MIN_FILE_SIZE.string, (value / 1024).toString()).apply()

    var lastModified: Long
        get() = pref.getLong(PREFERENCE.INTERNAL_LAST_MODIFIED.string, -1)
        set(value) = pref.edit().putLong(PREFERENCE.INTERNAL_LAST_MODIFIED.string, value).apply()

    var useMediaStore: Boolean
        get() = pref.getBoolean(PREFERENCE.PREFERENCE_USE_MEDIASTORE.string, true)
        set(value) = pref.edit().putBoolean(PREFERENCE.PREFERENCE_USE_MEDIASTORE.string, value).apply()

    fun registerOnChangeListener(preference: PREFERENCE, onChange: (it: Setting) -> Unit) {
        changeListeners[preference.string] = onChange
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences, p1: String) {
        changeListeners[p1]?.invoke(this)
    }
}