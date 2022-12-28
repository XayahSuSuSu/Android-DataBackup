package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.data.MediaInfoItem
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.GlobalString
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
                        var name = path.split("/").last()
                        for (i in viewModel.mediaInfoList) {
                            if (path == i.path) {
                                Toast.makeText(
                                    requireContext(),
                                    GlobalString.repeatToAdd,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            if (name == i.name) {
                                // 重名媒体资料
                                val nameList = name.split("_").toMutableList()
                                val index = nameList.last().toIntOrNull()
                                if (index == null) {
                                    nameList.add("0")
                                } else {
                                    nameList[nameList.lastIndex] = (index + 1).toString()
                                }
                                name = nameList.joinToString(separator = "_")
                            }
                        }
                        val mediaInfo = MediaInfo(
                            name = name,
                            path = path,
                            backup = MediaInfoItem(
                                data = true,
                                size = "",
                                date = ""
                            ),
                            _restoreIndex = -1,
                            restoreList = mutableListOf()
                        )
                        viewModel.mediaInfoList.add(mediaInfo)
                        addChip(mediaInfo)
                        App.saveMediaInfoList()
                    }
                }
            }
        }
        viewModel._isInitialized.observe(viewLifecycleOwner) {
            viewModel.viewModelScope.launch {
                if (it) {
                    setChipGroup()
                } else {
                    viewModel.refresh()
                }
            }
        }
    }

    private fun setChipGroup() {
        viewModel.viewModelScope.launch {
            binding.chipGroup.removeAllViews()
            for (i in viewModel.mediaInfoList) {
                addChip(i)
            }
            viewModel.lazyChipGroup.set(false)
        }
    }

    private fun addChip(mediaInfo: MediaInfo) {
        val chip = InputChip(layoutInflater, binding.chipGroup).apply {
            text = mediaInfo.name
            isChecked = mediaInfo.backup.data
            setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    mediaInfo.backup.data = isChecked
                    App.saveMediaInfoList()
                }
            }
            setOnCloseIconClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setWithConfirm("${GlobalString.confirmRemove}${GlobalString.symbolQuestion}") {
                        viewModel.viewModelScope.launch {
                            viewModel.mediaInfoList.remove(mediaInfo)
                            binding.chipGroup.removeView(it)
                            App.saveMediaInfoList()
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

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}