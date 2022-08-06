package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.setLoading
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.launch

class RestoreFragment : Fragment() {
    private var _binding: FragmentRestoreBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RestoreViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[RestoreViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel._isInitialized.observe(viewLifecycleOwner) {
            viewModel.viewModelScope.launch {
                if (it) {
                    setChipGroup()
                } else {
                    BottomSheetDialog(requireContext()).apply {
                        setLoading()
                        viewModel.refresh()
                        dismiss()
                    }
                }
            }
        }
    }

    private fun setChipGroup() {
        viewModel.viewModelScope.launch {
            binding.chipGroup.removeAllViews()
            for (i in viewModel.mediaInfoRestoreList) {
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
                    saveMediaInfoRestoreList()
                }
            }
            setOnCloseIconClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setWithConfirm(
                        "${GlobalString.confirmRemove}${GlobalString.symbolQuestion}\n" +
                                "${GlobalString.removeFilesToo}${GlobalString.symbolExclamation}"
                    ) {
                        viewModel.viewModelScope.launch {
                            Command.rm("${Path.getBackupMediaSavePath()}/${mediaInfo.name}.tar*")
                                .apply {
                                    if (this) {
                                        viewModel.mediaInfoRestoreList.remove(mediaInfo)
                                        binding.chipGroup.removeView(it)
                                        for (i in viewModel.mediaInfoRestoreList) {
                                            if (i.name == mediaInfo.name && i.path == mediaInfo.path) {
                                                // 清除媒体备份大小信息
                                                i.size = ""
                                                break
                                            }
                                        }
                                        saveMediaInfoRestoreList()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "${GlobalString.removeFailed}${GlobalString.symbolExclamation}",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }
                        }
                    }
                }
            }
        }
        binding.chipGroup.addView(chip)
    }

    private suspend fun saveMediaInfoRestoreList() {
        JSON.saveMediaInfoRestoreList(viewModel.mediaInfoRestoreList)
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