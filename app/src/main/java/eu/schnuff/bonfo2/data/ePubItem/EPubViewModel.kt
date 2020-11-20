package eu.schnuff.bonfo2.data.ePubItem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.schnuff.bonfo2.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EPubViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).ePubItemDao()

    fun get() = dao.getAll()

    fun insert(item: EPubItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.insert(item)
    }

    fun delete(item: EPubItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.delete(item)
    }

    fun delete(items: Collection<EPubItem>) = viewModelScope.launch(Dispatchers.IO) {
        dao.delete(items)
    }

    fun update(item: EPubItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.update(item)
    }
}