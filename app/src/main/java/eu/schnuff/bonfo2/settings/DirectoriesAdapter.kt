package eu.schnuff.bonfo2.settings

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.schnuff.bonfo2.helper.Setting

class DirectoriesAdapter(
    private val setting: Setting,
    private val onChange: (items: Array<String>) -> Unit
): RecyclerView.Adapter<DirectoriesAdapter.ViewHolder>() {
    private val watchedDirectories: Array<String>
            get() = setting.watchedDirectories.toTypedArray()

    init {
        setting.registerOnChangeListener {
            notifyDataSetChanged()
            onChange(watchedDirectories)
        }
        onChange(watchedDirectories)
    }
    class ViewHolder(private val v: TextView) : RecyclerView.ViewHolder(v) {
        var text: String
            get() = v.text.toString()
            set(value) { v.text = value }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = text_layout
        }
        return ViewHolder(textView)
    }

    override fun getItemCount() = watchedDirectories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text = watchedDirectories[position]
    }

    companion object {
        val text_layout = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}