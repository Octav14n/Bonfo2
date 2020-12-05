package eu.schnuff.bonfo2.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import eu.schnuff.bonfo2.R
import eu.schnuff.bonfo2.databinding.SortBinding
import eu.schnuff.bonfo2.helper.Setting
import eu.schnuff.bonfo2.helper.SortBy
import eu.schnuff.bonfo2.helper.SortOrder

class SortDialog(private val onAcceptListener: (it: SortDialog) -> Unit) : DialogFragment() {
    private lateinit var binding: SortBinding
    val sortBy
    get() = when {
        binding.sortByCreation.isChecked -> SortBy.CREATION
        binding.sortByAccess.isChecked -> SortBy.ACCESS
        else -> throw IllegalStateException("None of the radio-boxes selected.")
    }
    val showSmall
    get() = binding.filterBySmall.isChecked
    val showNsfw
    get() = binding.filterShowNsfw.isChecked
    val sortOrder
    get() = when (binding.sortOrder.checkedButtonId) {
        binding.sortAsc.id -> SortOrder.ASC
        binding.sortDesc.id -> SortOrder.DESC
        else -> throw IllegalStateException("Neither ASC nor DESC was selected.")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = SortBinding.inflate(layoutInflater)
            val s = Setting(requireContext())

            val builder = AlertDialog.Builder(it).apply {
                setTitle(R.string.action_sort)
                setView(binding.root)
                setPositiveButton(R.string.action_sort_apply) { _, _ ->
                    setSettingsFromState(binding, s)
                    onAcceptListener(this@SortDialog)
                }
            }

            setStateFromSettings(binding, s)

            builder.create()
        } ?: throw IllegalStateException("Activity cannot not be null")
    }

    private fun setStateFromSettings(binding: SortBinding, s: Setting) {
        when (s.sortBy) {
            SortBy.CREATION -> binding.sortByCreation.isChecked = true
            SortBy.ACCESS -> binding.sortByAccess.isChecked = true
        }
        binding.sortOrder.check(when (s.sortOrder) {
            SortOrder.ASC -> binding.sortAsc.id
            SortOrder.DESC -> binding.sortDesc.id
        })
        binding.filterBySmall.isChecked = s.showSmall
        binding.filterShowNsfw.isChecked = s.showNsfw
    }

    private fun setSettingsFromState(binding: SortBinding, s: Setting) {
        s.sortBy = sortBy
        s.sortOrder = sortOrder
        s.showSmall = showSmall
        s.showNsfw = showNsfw
    }

}