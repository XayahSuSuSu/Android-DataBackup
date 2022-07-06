package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.App
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.saveBackupSavePath
import com.xayah.databackup.view.InputChip

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
        App.globalContext.saveBackupSavePath(binding.storageRadioCard.getPathByIndex(binding.storageRadioCard.radioGroupCheckedIndex))
        binding.storageRadioCard.setOnCheckedChangeListener { _, path ->
            initialize()
        }

        binding.chipGroup.removeAllViews()
        val mediaInfoList = Command.getCachedMediaInfoRestoreList()
        for (i in mediaInfoList) {
            addChip(i, mediaInfoList)
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
            Path.getMediaInfoRestoreListPath()
        )
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