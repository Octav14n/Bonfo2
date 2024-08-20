package eu.schnuff.bonfo2.data.ePubItem;

import android.content.Context;
import eu.schnuff.bonfo2.data.AppDatabase

public class EPubItems(
    private val context: Context
) {
    private val dao = AppDatabase.getDatabase(context).ePubItemDao()
}
