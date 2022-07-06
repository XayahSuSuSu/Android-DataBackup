package com.xayah.databackup.fragment.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.App
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentBackupBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.saveBackupSavePath
import com.xayah.databackup.view.InputChip
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer


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
        App.globalContext.saveBackupSavePath(binding.storageRadioCard.getPathByIndex(binding.storageRadioCard.radioGroupCheckedIndex))
        binding.storageRadioCard.setOnCheckedChangeListener { _, path ->
            initialize()
        }

        binding.chipGroup.removeAllViews()
        val mediaInfoList = Command.getCachedMediaInfoBackupList()
        for (i in mediaInfoList) {
            addChip(i, mediaInfoList)
        }
        binding.materialButtonAddMedia.setOnClickListener {
            materialYouFileExplorer.apply {
                isFile = false
                toExplorer(requireContext()) { path, _ ->
                    val mediaInfo = MediaInfo(path.split("/").last(), path, true, "")
                    mediaInfoList.add(mediaInfo)
                    addChip(mediaInfo, mediaInfoList)
                    saveMediaList(mediaInfoList)
                }
            }
        }

        viewModel.initialize()
    }

    private fun addChip(mediaInfo: MediaInfo, mediaInfoList: MutableList<MediaInfo>) {
        val chip = InputChip(layoutInflater, binding.chipGroup).apply {
            text = mediaInfo.name
            isChecked = mediaInfo.data
            setOnCheckedChangeListener { _, isChecked ->
                mediaInfo.data = isChecked
                saveMediaList(mediaInfoList)
            }
            setOnCloseIconClickListener {
                mediaInfoList.remove(mediaInfo)
                binding.chipGroup.removeView(this)
                saveMediaList(mediaInfoList)
            }
        }
        binding.chipGroup.addView(chip)
    }

    private fun saveMediaList(mediaInfoList: MutableList<MediaInfo>) {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mediaInfoList as MutableList<Any>),
            Path.getMediaInfoBackupListPath()
        )
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