package eu.schnuff.bonfo2.helper

import android.content.Context
import androidx.preference.PreferenceManager
private const val PREFERENCE_WATCHED_DIRECTORIES = "watched_directories"
private const val PREFERENCE_LIST_SCROLL_IDX = "list_first_item_idx"
private const val PREFERENCE_LIST_FILTER = "list_filter"

class Setting(private val context: Context, onChange: (it: Setting) -> Unit = {}) {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        pref.registerOnSharedPreferenceChangeListener { _, _ ->
            onChange(this)
        }
    }

    val watchedDirectories: Set<String>
        get() = pref.getStringSet(PREFERENCE_WATCHED_DIRECTORIES, emptySet()).orEmpty()

    var listScrollIdx: Int
        get() = pref.getInt(PREFERENCE_LIST_SCROLL_IDX, 0)
        set(value) = pref.edit().putInt(PREFERENCE_LIST_SCROLL_IDX, value).apply()

    var filter: String
        get() = pref.getString(PREFERENCE_LIST_FILTER, "") ?: ""
        set(value) = pref.edit().putString(PREFERENCE_LIST_FILTER, value).apply()
}