package eu.schnuff.bonfo2.settings

import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import eu.schnuff.bonfo2.helper.Setting
import eu.schnuff.bonfo2.helper.withFilePermission
import kotlinx.android.synthetic.main.preference_directories.view.*

class DirectoriesPreference (context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    var onAddListener: (v: View) -> Unit = {}
    val setting = Setting(context)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val adapter = DirectoriesAdapter(context) {
            val b = it.isEmpty()
            holder.itemView.pref_dir_empty.visibility = if(b) View.VISIBLE else View.GONE
            holder.itemView.pref_dir.visibility = if (b) View.GONE else View.VISIBLE
        }
        holder.itemView.pref_dir_add ?: TODO("PreferenceDirLister has no ui.")
        holder.itemView.pref_dir_add.setOnClickListener {
            onAddListener(it)

            context.withFilePermission {
                MaterialDialog(context).show {
                    folderChooser(context, initialDirectory = Environment.getExternalStorageDirectory()) { _, folder ->
                        Log.d(this@DirectoriesPreference::class.qualifiedName, "folder chooser: %s".format(folder))
                        persistStringSet(
                            (getPersistedStringSet(emptySet()) ?: emptySet()).toMutableSet().also { set ->
                                set.add(folder.absolutePath)
                            }
                        )
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        val list = holder.itemView.pref_dir
        list.adapter = adapter
        holder.itemView.visibility = View.VISIBLE
    }
}