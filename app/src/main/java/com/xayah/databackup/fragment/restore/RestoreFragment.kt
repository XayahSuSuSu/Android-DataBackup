package com.xayah.databackup.fragment.restore

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
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.InputChip
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
        if (mediaInfo.restoreList.isNotEmpty()) {
            val chip = InputChip(layoutInflater, binding.chipGroup).apply {
                text = mediaInfo.name
                isChecked = mediaInfo.restoreList[mediaInfo.restoreIndex].data
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.viewModelScope.launch {
                        mediaInfo.restoreList[mediaInfo.restoreIndex].data = isChecked
                        App.saveMediaInfoList()
                    }
                }
                setOnCloseIconClickListener {
                    MaterialAlertDialogBuilder(requireContext()).apply {
                        setWithConfirm(
                            "${GlobalString.confirmRemove}${GlobalString.symbolQuestion}\n" +
                                    "${GlobalString.removeFilesToo}${GlobalString.symbolExclamation}"
                        ) {
//                            viewModel.viewModelScope.launch {
//                                Command.rm("${Path.getBackupMediaSavePath()}/${mediaInfo.name}.tar*")
//                                    .apply {
//                                        if (this) {
//                                            viewModel.mediaInfoRestoreList.remove(mediaInfo)
//                                            binding.chipGroup.removeView(it)
//                                            for (i in viewModel.mediaInfoBackupList) {
//                                                if (i.name == mediaInfo.name && i.path == mediaInfo.path) {
//                                                    // 清除媒体备份大小信息
//                                                    i.size = ""
//                                                    break
//                                                }
//                                            }
//                                            for (i in viewModel.mediaInfoRestoreList) {
//                                                if (i.name == mediaInfo.name && i.path == mediaInfo.path) {
//                                                    // 清除媒体备份大小信息
//                                                    i.size = ""
//                                                    break
//                                                }
//                                            }
//                                            App.saveMediaInfoList()
//                                        } else {
//                                            Toast.makeText(
//                                                requireContext(),
//                                                "${GlobalString.removeFailed}${GlobalString.symbolExclamation}",
//                                                Toast.LENGTH_SHORT
//                                            )
//                                                .show()
//                                        }
//                                    }
//                            }
                        }
                    }
                }
            }
            binding.chipGroup.addView(chip)
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