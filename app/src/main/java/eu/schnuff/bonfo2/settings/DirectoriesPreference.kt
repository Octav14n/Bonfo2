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
import eu.schnuff.bonfo2.databinding.PreferenceDirectoriesBinding
import eu.schnuff.bonfo2.helper.Setting
import eu.schnuff.bonfo2.helper.withFilePermission

class DirectoriesPreference (context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    var onAddListener: (v: View) -> Unit = {}
    val setting = Setting(context)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binder = PreferenceDirectoriesBinding.bind(holder.itemView)

        val adapter = DirectoriesAdapter(setting) {
            val b = it.isEmpty()
            binder.prefDirEmpty.visibility = if(b) View.VISIBLE else View.GONE
            binder.prefDir.visibility = if (b) View.GONE else View.VISIBLE
        }
        binder.prefDirAdd.setOnClickListener {
            onAddListener(it)

            context.withFilePermission {
                MaterialDialog(context).show {
                    folderChooser(context, initialDirectory = Environment.getExternalStorageDirectory()) { _, folder ->
                        Log.d(TAG, "folder chooser: $folder")
                        setting.addWatchedDirectory(folder.absolutePath)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        binder.prefDir.adapter = adapter
        holder.itemView.visibility = View.VISIBLE
    }

    companion object {
        val TAG = this::class.qualifiedName
    }
}