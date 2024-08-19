package eu.schnuff.bonfo2.settings

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.AppDatabase
import eu.schnuff.bonfo2.databinding.SettingsActivityBinding
import eu.schnuff.bonfo2.helper.PREFERENCE
import eu.schnuff.bonfo2.helper.Setting
import eu.schnuff.bonfo2.helper.withFilePermission
import kotlin.concurrent.thread

private const val ACTIVITY_FOLDER_SELECT_ID = 3000

class SettingsMain : AppCompatActivity() {
    private lateinit var binding: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var watchedDirectories: DirectoriesPreference
        private lateinit var setting: Setting

        private val folderSelectResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            val data = result.data
            val uri = data?.data
            if (result.resultCode != RESULT_OK || data == null || uri == null)
                return@registerForActivityResult

            DocumentFile.fromTreeUri(requireContext(), uri)?.let {
                val takeFlags: Int = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

                val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(it.uri, DocumentsContract.getTreeDocumentId(it.uri))

                watchedDirectories.addDirectory(childUri.toString())
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setting = Setting(requireContext())
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            watchedDirectories = findPreference(PREFERENCE.PREFERENCE_WATCHED_DIRECTORIES.string)!!
            watchedDirectories.onAddListener = {
                MaterialDialog(requireContext()).show {
                    title(R.string.pref_dir_select_title)
                    message(R.string.pref_dir_select_info)
                    positiveButton(R.string.pref_dir_select_saf) { openDirDialogSAF() }
                    negativeButton(R.string.pref_dir_select_native) { openDirDialogNative() }
                }
            }
        }

        private fun openDirDialogSAF() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            folderSelectResultLauncher.launch(intent)
        }

        private fun openDirDialogNative() {
            requireContext().withFilePermission {
                MaterialDialog(requireContext()).show {
                    folderChooser(context, initialDirectory = Environment.getExternalStorageDirectory()) { _, folder ->
                        Log.d(DirectoriesPreference.TAG, "folder chooser: $folder")
                        setting.addWatchedDirectory(folder.toUri().toString())
                    }
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "developer_items_empty" -> thread { AppDatabase.getDatabase(requireContext()).ePubItemDao().devDeleteAll() }
                "reset_last_modified" -> {
                    setting.lastModified = -1
                    setting.lastModifiedMediaGeneration = -1
                    setting.lastMediaStoreVersion = null
                }
                else -> return false
            }
            return true
        }
    }
}