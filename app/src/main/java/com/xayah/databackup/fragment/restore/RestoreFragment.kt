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
import com.xayah.databackup.util.*
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.util.setWithConfirm

class RestoreFragment : Fragment() {
    private var _binding: FragmentRestoreBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RestoreViewModel
    private var mediaInfoList: MutableList<MediaInfo> = mutableListOf()

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
        binding.storageRadioCard.setOnCheckedChangeListener { _, _ ->
            initialize()
        }

        viewModel.initialize { setChipGroup() }
    }

    private fun setChipGroup() {
        binding.chipGroup.removeAllViews()
        mediaInfoList = Command.getCachedMediaInfoRestoreList()
        for (i in mediaInfoList) {
            addChip(i, mediaInfoList)
        }
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
                MaterialAlertDialogBuilder(requireContext()).apply {
                    setWithConfirm(
                        "${GlobalString.confirmRemove}${GlobalString.symbolQuestion}\n" +
                                "${GlobalString.removeFilesToo}${GlobalString.symbolExclamation}"
                    ) {
                        Command.rm("${Path.getBackupMediaSavePath()}/${mediaInfo.name}.tar.*")
                            .apply {
                                if (this) {
                                    mediaInfoList.remove(mediaInfo)
                                    binding.chipGroup.removeView(it)
                                    saveMediaList(mediaInfoList)
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