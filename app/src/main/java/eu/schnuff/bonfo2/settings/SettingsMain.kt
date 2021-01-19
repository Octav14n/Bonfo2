package eu.schnuff.bonfo2.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.data.AppDatabase
import eu.schnuff.bonfo2.databinding.SettingsActivityBinding
import kotlin.concurrent.thread

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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            this.findPreference<Preference>("developer_items_empty")!!.setOnPreferenceClickListener {
                thread {
                    AppDatabase.getDatabase(requireContext()).ePubItemDao().devDeleteAll()
                }
                true
            }
        }


    }
}