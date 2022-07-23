package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.util.setWithConfirm
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    }

    private fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.initialize()
        }
        binding.materialButtonChangeBackupUser.setOnClickListener {
            viewModel.onChangeUser(it) {
                initialize()
            }
        }
        binding.materialButtonAddMedia.setOnClickListener {
            materialYouFileExplorer.apply {
                isFile = false
                toExplorer(requireContext()) { path, _ ->
                    val mediaInfo = MediaInfo(path.split("/").last(), path, true, "")
                    getMediaInfoList().add(mediaInfo)
                    addChip(mediaInfo)
                }
            }
        }
        setChipGroup()
    }

    private fun setChipGroup() {
        binding.chipGroup.removeAllViews()
        for (i in getMediaInfoList()) {
            addChip(i)
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
                    setWithConfirm("${GlobalString.confirmRemove}${GlobalString.symbolQuestion}") {
                        getMediaInfoList().remove(mediaInfo)
                        binding.chipGroup.removeView(it)
                    }
                }
            }
        }
        binding.chipGroup.addView(chip)
    }

    private fun getMediaInfoList(): MutableList<MediaInfo> {
        return App.globalMediaInfoBackupList
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
        initialize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}