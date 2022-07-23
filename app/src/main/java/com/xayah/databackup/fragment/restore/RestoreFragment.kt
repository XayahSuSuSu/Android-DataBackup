package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    }

    private fun initialize() {
        binding.materialButtonChangeRestoreUser.setOnClickListener {
            viewModel.onChangeUser(it)
        }
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.initialize { setChipGroup() }
        }
    }

    private fun setChipGroup() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.chipGroup.removeAllViews()
            for (i in getMediaInfoList()) {
                addChip(i)
            }
        }
    }

    private fun addChip(mediaInfo: MediaInfo) {
        val chip = InputChip(layoutInflater, binding.chipGroup).apply {
            text = mediaInfo.name
            isChecked = mediaInfo.data
            setOnCheckedChangeListener { _, isChecked ->
                mediaInfo.data = isChecked
            }
            setOnCloseIconClickListener {
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setWithConfirm(
                        "${GlobalString.confirmRemove}${GlobalString.symbolQuestion}\n" +
                                "${GlobalString.removeFilesToo}${GlobalString.symbolExclamation}"
                    ) {
                        Command.rm("${Path.getBackupMediaSavePath()}/${mediaInfo.name}.tar*")
                            .apply {
                                if (this) {
                                    getMediaInfoList().remove(mediaInfo)
                                    binding.chipGroup.removeView(it)
                                    for (i in App.globalMediaInfoBackupList) {
                                        if (i.name == mediaInfo.name && i.path == mediaInfo.path) {
                                            // 清除媒体备份大小信息
                                            i.size = ""
                                            break
                                        }
                                    }
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
        binding.chipGroup.addView(chip)
    }

    private fun getMediaInfoList(): MutableList<MediaInfo> {
        return App.globalMediaInfoRestoreList
    }

    override fun onResume() {
        super.onResume()
        initialize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}