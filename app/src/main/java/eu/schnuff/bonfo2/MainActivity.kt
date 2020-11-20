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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.data.ePubItem.EPubViewModel
import eu.schnuff.bonfo2.data.historyItem.ACTION
import eu.schnuff.bonfo2.data.historyItem.HistoryItem
import eu.schnuff.bonfo2.data.historyItem.HistoryViewModel
import eu.schnuff.bonfo2.helper.Setting
import eu.schnuff.bonfo2.helper.withFilePermission
import eu.schnuff.bonfo2.list.BookAdapter
import eu.schnuff.bonfo2.settings.SettingsMain
import eu.schnuff.bonfo2.update.UpdateService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Integer.min
import java.lang.reflect.Method


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener, ServiceConnection {
    private var updateService: UpdateService? = null
    private val adapter = BookAdapter(
        onClickListener = this::onListItemClick,
        onListChanged = { _, newList ->
            if (newList.isNotEmpty() && !firstListObserved) {
                firstListObserved = true
                listClear.visibility = View.GONE
                list.visibility = View.VISIBLE
                (list.layoutManager!! as LinearLayoutManager).scrollToPosition(setting.listScrollIdx)
            }
        }
    )
    private var firstListObserved: Boolean = false
    private lateinit var searchView: SearchView
    private lateinit var setting: Setting
    private lateinit var ePubViewModel: EPubViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private var isRefreshing: Boolean = false
        set(value) {
            field = value
            refresh.isRefreshing = value
            progressBar.visibility = if (value) {
                progressBar.isIndeterminate = true
                View.VISIBLE
            } else View.GONE
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
        BookAdapter.activity = this
        ePubViewModel = EPubViewModel(application)
        historyViewModel = HistoryViewModel(application)
        setting = Setting(this).also {
            adapter.filter(it.filter)
        }
        // Disable file uri errors
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                val m: Method = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        refresh.setOnRefreshListener(this)
        ePubViewModel.get().observe(this, Observer {
            list -> adapter.submitList(list)
        })
        historyViewModel.get().observe(this, Observer { list -> adapter.lastOpened = list.subList(0, min(2, list.size)).map { it.item } })
        list.adapter = adapter
    }

    override fun onPause() {
        // Save UI user input
        setting.listScrollIdx = (list.layoutManager!! as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        setting.filter = searchView.query.toString()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        updateService?.run {
            isRefreshing = progressing.value ?: false
        }
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
        menu?.also {
            searchView = it.findItem(R.id.action_search).actionView as SearchView
            searchView.setOnQueryTextListener(this)
            val filter = setting.filter
            if (filter.isNotEmpty()) {
                searchView.setQuery(filter, false)
                searchView.isIconified = false
                searchView.clearFocus()
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    fun startSettings(v: View) = startSettings()
    private fun startSettings() {
        startActivity(Intent(this, SettingsMain::class.java))
    }

    private fun showSort() {

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
            progressing.observe(this@MainActivity, Observer {
                isRefreshing = it

            })
            progressMax.observe(this@MainActivity, Observer {
                progressBar.max = it
            })
            progressNow.observe(this@MainActivity, Observer {
                progressBar.isIndeterminate = false
                progressBar.progress = it
            })
        }
    }
}
