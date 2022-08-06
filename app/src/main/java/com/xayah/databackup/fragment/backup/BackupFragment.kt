package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.JSON
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.util.setWithConfirm
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.launch


class BackupFragment : Fragment() {
    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BackupViewModel
    private lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[BackupViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.materialButtonAddMedia.setOnClickListener {
            materialYouFileExplorer.apply {
                isFile = false
                toExplorer(requireContext()) { path, _ ->
                    viewModel.viewModelScope.launch {
                        val mediaInfo = MediaInfo(path.split("/").last(), path, true, "")
                        viewModel.mediaInfoBackupList.add(mediaInfo)
                        addChip(mediaInfo)
                        saveMediaInfoBackupList()
                    }
                }
            }
        }
        viewModel._isInitialized.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.viewModelScope.launch {
                    setChipGroup()
                }
            } else
                viewModel.viewModelScope.launch {
                    viewModel.refresh()
                }
        }
    }

    private fun setChipGroup() {
        viewModel.viewModelScope.launch {
            binding.chipGroup.removeAllViews()
            for (i in viewModel.mediaInfoBackupList) {
                addChip(i)
            }
        }
    }

    private fun addChip(mediaInfo: MediaInfo) {
        val chip = InputChip(layoutInflater, binding.chipGroup).apply {
            text = mediaInfo.name
            isChecked = mediaInfo.data
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    mediaInfo.data = isChecked
                    saveMediaInfoBackupList()
                }
            }
            setOnCloseIconClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setWithConfirm("${GlobalString.confirmRemove}${GlobalString.symbolQuestion}") {
                        viewModel.viewModelScope.launch {
                            viewModel.mediaInfoBackupList.remove(mediaInfo)
                            binding.chipGroup.removeView(it)
                            saveMediaInfoBackupList()
                        }
                    }
                }
            }
        }
        binding.chipGroup.addView(chip)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val that = this
        materialYouFileExplorer = MaterialYouFileExplorer().apply {
            initialize(that)
        }
    }

    private suspend fun saveMediaInfoBackupList() {
        JSON.saveMediaInfoBackupList(viewModel.mediaInfoBackupList)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}