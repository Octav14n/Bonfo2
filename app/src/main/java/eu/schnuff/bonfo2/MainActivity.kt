package eu.schnuff.bonfo2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.data.ePubItem.EPubViewModel
import eu.schnuff.bonfo2.data.historyItem.ACTION
import eu.schnuff.bonfo2.data.historyItem.HistoryItem
import eu.schnuff.bonfo2.data.historyItem.HistoryViewModel
import eu.schnuff.bonfo2.databinding.ActivityMainBinding
import eu.schnuff.bonfo2.dialogs.SortDialog
import eu.schnuff.bonfo2.filter.Filter
import eu.schnuff.bonfo2.helper.Setting
import eu.schnuff.bonfo2.helper.withFilePermission
import eu.schnuff.bonfo2.list.BookAdapter
import eu.schnuff.bonfo2.settings.SettingsMain
import eu.schnuff.bonfo2.update.UpdateService
import java.io.File


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener, ServiceConnection {
    private lateinit var binding: ActivityMainBinding
    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView
    private lateinit var setting: Setting
    private lateinit var ePubViewModel: EPubViewModel
    private lateinit var historyViewModel: HistoryViewModel

    private var updateService: UpdateService? = null
    private val adapter = BookAdapter(
        onClickListener = this::onListItemClick,
        onListChanged = { _, newList ->
            if (newList.isNotEmpty() && !firstListObserved) {
                firstListObserved = true
                binding.listClear.visibility = View.GONE
                binding.list.visibility = View.VISIBLE
                (binding.list.layoutManager!! as LinearLayoutManager).scrollToPosition(setting.listScrollIdx)
            }
        },
        onLongClickListener = { item ->
            AlertDialog.Builder(this).apply {
                setItems(R.array.actions) { _, i -> when (i) {
                    0 -> onListItemClick(item)
                    1 -> filter = item.author?.replace(Filter.FILTER_SPLITTER, Filter.FILTER_REPLACER) ?: ""
                    2 -> item.webUrl?.run { startActivity(Intent(Intent.ACTION_VIEW, toUri())) }
                    3 -> {
                        item.webUrl ?: return@setItems
                        val intent = packageManager.getLaunchIntentForPackage("eu.schnuff.bofilo")?.apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, item.webUrl)
                            type = "text/plain"
                            // putExtra(Intent.EXTRA_COMPONENT_NAME, item.fileName)
                        } ?: return@setItems
                        startActivity(intent)
                    }
                }}
            }.show()
        }
    )
    private var firstListObserved: Boolean = false
    private var isRefreshing: Boolean = false
        set(value) {
            field = value
            binding.refresh.isRefreshing = value
            binding.progressBar.visibility = if (value) {
                binding.progressBar.isIndeterminate = true
                View.VISIBLE
            } else View.GONE
        }
    private var filter
        get() = searchView.query.toString()
        set(value) {
            if (value.isEmpty()) {
                searchMenuItem.collapseActionView()
                searchView.isIconified = true
            } else {
                searchMenuItem.expandActionView()
                searchView.isIconified = false
                searchView.setQuery(value, false)
                searchView.clearFocus()
            }
        }


    private fun onListItemClick(it: EPubItem) {
        historyViewModel.insert(HistoryItem(it.url, ACTION.VIEW))

        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(File(it.filePath)), "application/epub+zip")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_cant_open_view_ebook, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // LateInit Initializations
        binding = ActivityMainBinding.inflate(layoutInflater)
        ePubViewModel = EPubViewModel(application)
        historyViewModel = HistoryViewModel(application)
        setting = Setting(this).also {
            adapter.filter(it.filter)
        }
        // Disable file uri errors
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                StrictMode::class.java.getMethod("disableDeathOnFileUriExposure").invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        applyFilterFromSetting()
        adapter.filter(setting.filter)

        binding.refresh.setOnRefreshListener(this)
        ePubViewModel.get().observe(this, {
            list -> adapter.submitList(list)
        })
        historyViewModel.get().observe(this, { list -> adapter.lastOpened = list.map { it.item } })
        binding.list.adapter = adapter
    }

    override fun onPause() {
        // Save UI user input
        setting.listScrollIdx = (binding.list.layoutManager!! as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        setting.filter = filter
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (updateService == null)
            isRefreshing = false
    }

    override fun onDestroy() {
        if (updateService != null)
            unbindService(this)
        super.onDestroy()
    }

    fun onRefresh(v: View) = onRefresh()
    override fun onRefresh() = withFilePermission {
        startUpdateService()
    }

    private fun startUpdateService() {
        val intent = Intent(this, UpdateService::class.java).apply {
            action = UpdateService.ACTION_START
        }
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu ?: throw IllegalStateException("Menu not found")

        searchMenuItem = menu.findItem(R.id.action_search)
        searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        filter = setting.filter

        return super.onCreateOptionsMenu(menu)
    }

    fun startSettings(v: View) = startSettings()
    private fun startSettings() {
        startActivity(Intent(this, SettingsMain::class.java))
    }

    private fun showSort() {
        val dialog = SortDialog {
            applyFilterFromSetting()
        }
        dialog.show(supportFragmentManager, SORT_DIALOG_TAG)
    }

    private fun applyFilterFromSetting() {
        adapter.filter.minFileSize = if (setting.showSmall) -1 else setting.minFileSize
        adapter.filter.excludeGenres = if (setting.showNsfw) emptySet() else this.getStringArray(R.array.nsfw_genres).toSet()
        adapter.setSort(setting.sortBy, setting.sortOrder)
    }

    override fun onQueryTextSubmit(query: String?): Boolean { return false }
    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null)
            adapter.filter(newText)
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startSettings()
            R.id.action_refresh -> onRefresh()
            R.id.action_sort -> showSort()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onServiceDisconnected(name: ComponentName?) { updateService = null; isRefreshing = false }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        updateService = (service as? UpdateService.UpdateBinder)?.Service
        updateService?.run {
            isRefreshing = progressing.value ?: false
            progressing.observe(this@MainActivity) {
                isRefreshing = it
            }
            progressMax.observe(this@MainActivity) {
                binding.progressBar.max = it
            }
            progressNow.observe(this@MainActivity) {
                binding.progressBar.isIndeterminate = false
                binding.progressBar.progress = it
            }
        }
    }

    companion object {
        const val SORT_DIALOG_TAG = "sort_dialog_tag"
    }
}
