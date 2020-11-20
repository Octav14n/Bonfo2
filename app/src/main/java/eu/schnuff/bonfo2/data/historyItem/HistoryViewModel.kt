package eu.schnuff.bonfo2.data.historyItem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.schnuff.bonfo2.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).historyItemDao()

    fun get() = dao.getAll()

    fun insert(item: HistoryItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.insert(item)
    }
}