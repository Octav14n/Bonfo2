package eu.schnuff.bonfo2

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val REFRESHING_RESET_TIME = 750L

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener, ServiceConnection {
    private lateinit var binding: ActivityMainBinding
    private lateinit var searchMenuItem: MenuItem
    private lateinit var searchView: SearchView
    private lateinit var setting: Setting
    private lateinit var ePubViewModel: EPubViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var refreshingResetHandler: Handler

    private var updateService: UpdateService? = null
    private val adapter = BookAdapter(
        onClickListener = this::onListItemClick,
        onLongClickListener = { item ->
            AlertDialog.Builder(this).apply {
                setItems(R.array.actions) { _, i -> when (i) {
                    0 -> onListItemClick(item)
                    1 -> filter = item.author?.replace(Filter.FILTER_SPLITTER, Filter.FILTER_REPLACER) ?: ""
                    2 -> item.webUrl?.run { startActivity(Intent(Intent.ACTION_VIEW, toUri())) }
                    3 -> {
                        item.webUrl ?: return@setItems
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            `package` = "eu.schnuff.bofilo"
                            putExtra(Intent.EXTRA_TEXT, item.webUrl)
                            type = "text/plain"
                        }
                        startActivity(intent)
                    }
                    4 -> {
                        //packageManager.queryIntentActivities()
                        val intent = Intent("eu.schnuff.bofilo.action.unnew").apply {
                            putExtra(Intent.EXTRA_TEXT, item.filePath.toUri().toString())
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(this::class.simpleName, "Can not start unnew.", e)
                            Toast.makeText(this@MainActivity, "BoFiLo is not installed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }}
            }.show()
        }
    )
    private var firstListObserved: Boolean = false
    private var isRefreshing: Boolean = false
        set(value) {
            field = value
            val visibility = if (value) {
                View.VISIBLE
            } else View.GONE
            if (binding.refresh.isRefreshing != value)
                binding.refresh.isRefreshing = value
            if (binding.progressBar.visibility != visibility) {
                binding.progressBar.visibility = visibility
                binding.progressBar.isIndeterminate = true
            }
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
    private val refreshingResetRunnable = Runnable {
        isRefreshing = false
    }


    private fun onListItemClick(it: EPubItem) {
        historyViewModel.insert(HistoryItem(it.url, ACTION.VIEW))

        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(it.filePath), "application/epub+zip")
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
        adapter.init(this)
        setting = Setting(this).also {
            adapter.filter(it.filter)
        }
        refreshingResetHandler = Handler(mainLooper)
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
        //ePubViewModel.get().observe(this) { list ->
        //    adapter.submitList(list)
        //}
        lifecycleScope.launch {
            launch {
                adapter.loadStateFlow.collectLatest {
                    binding.refresh.isRefreshing = it.refresh is LoadState.Loading
                    if (it.refresh !is LoadState.Loading && adapter.itemCount > 0) {
                        firstListObserved = true
                        binding.listClear.visibility = View.GONE
                        binding.list.visibility = View.VISIBLE
                        (binding.list.layoutManager!! as LinearLayoutManager).scrollToPosition(setting.listScrollIdx)
                    }
                }
            }
            launch { adapter.fetchData() }
        }
        historyViewModel.get().observe(this) {
            list -> adapter.lastOpened = list.map { it.item }
        }
        binding.list.adapter = adapter

        bindService(Intent(this, UpdateService::class.java), this, 0)
    }

    override fun onPause() {
        // Save UI user input
        try {
            setting.filter = filter
            setting.listScrollIdx =
                (binding.list.layoutManager!! as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        } catch (_: Exception) {}
        super.onPause()
    }

    override fun onResume() {
        //refreshingResetHandler.removeCallbacks(refreshingResetRunnable)
        //refreshingResetHandler.postDelayed(refreshingResetRunnable, REFRESHING_RESET_TIME)
        //if (updateService == null)

        isRefreshing = false
        super.onResume()
    }

    override fun onDestroy() {
        if (updateService != null)
            unbindService(this)
        super.onDestroy()
    }

    //fun onRefresh(v: View) = onRefresh()
    override fun onRefresh(): Unit = withFilePermission {
        val permissions = packageManager
            .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            permissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) &&
            //!setting.useMediaStore &&
            !Environment.isExternalStorageManager()
        ) {
            //request for the permission
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        } else {
            startUpdateService()
        }
    }

    private fun startUpdateService() {
        val intent = Intent(this, UpdateService::class.java).apply {
            action = UpdateService.ACTION_START
        }
        // bindService(intent, this, Context.BIND_AUTO_CREATE)
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
        lifecycleScope.launch {
            adapter.setSort(setting.sortBy, setting.sortOrder)
        }
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
            var max = 0
            progressing.observe(this@MainActivity) {
                isRefreshing = it
            }
            progressMax.observe(this@MainActivity) {
                binding.progressBar.max = it
                max = it
            }
            progressNow.observe(this@MainActivity) {
                binding.progressBar.isIndeterminate = false
                binding.progressBar.progress = it
                isRefreshing = true
                /*if (it < max) {
                    isRefreshing = true
                    refreshingResetHandler.removeCallbacks(refreshingResetRunnable)
                    refreshingResetHandler.postDelayed(refreshingResetRunnable, REFRESHING_RESET_TIME)
                } else {
                    isRefreshing = false
                }*/
            }
        }
    }

    companion object {
        const val SORT_DIALOG_TAG = "sort_dialog_tag"
    }
}
