package com.xayah.databackup.fragment.restore

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.data.MediaInfoRestore
import com.xayah.databackup.databinding.BottomSheetMediaDetailBinding
import com.xayah.databackup.databinding.FragmentRestoreBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.setWithTopBar
import com.xayah.databackup.view.title
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
        binding.lifecycleOwner = requireActivity()

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
            if (viewModel.globalObject.mediaInfoRestoreMap.value.isEmpty()) {
                GlobalObject.getInstance().mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
            }
            try {
                binding.chipGroup.removeAllViews()
                for (i in viewModel.mediaInfoRestoreMap.values) {
                    if (i.detailRestoreList.isNotEmpty())
                        addChip(i)
                }
                viewModel.lazyChipGroup.set(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addChip(mediaInfo: MediaInfoRestore) {
        if (mediaInfo.detailRestoreList.isNotEmpty()) {
            val chip = InputChip(layoutInflater, binding.chipGroup).apply {
                text = mediaInfo.name
                isChecked = mediaInfo.detailRestoreList[mediaInfo.restoreIndex].data
                isCloseIconVisible = false
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.viewModelScope.launch {
                        mediaInfo.detailRestoreList[mediaInfo.restoreIndex].data = isChecked
                        GsonUtil.saveMediaInfoRestoreMapToFile(viewModel.globalObject.mediaInfoRestoreMap.value)
                    }
                }
                setOnLongClickListener {
                    BottomSheetDialog(requireContext()).apply {
                        setWithTopBar().apply {
                            addView(title(mediaInfo.name))
                            addView(
                                BottomSheetMediaDetailBinding.inflate(
                                    LayoutInflater.from(requireContext()),
                                    null,
                                    false
                                ).apply {
                                    textInputEditText.apply {
                                        inputType = InputType.TYPE_NULL
                                        setText(mediaInfo.path)
                                    }
                                    if (mediaInfo.detailRestoreList.isNotEmpty()) {
                                        chipDate.apply {
                                            visibility = View.VISIBLE
                                            text =
                                                Command.getDate(mediaInfo.detailRestoreList[mediaInfo.restoreIndex].date)
                                            setOnClickListener {
                                                val choice = mediaInfo.restoreIndex
                                                val items = mutableListOf<String>()
                                                mediaInfo.detailRestoreList.forEach {
                                                    items.add(
                                                        Command.getDate(
                                                            it.date
                                                        )
                                                    )
                                                }

                                                ListPopupWindow(context).apply {
                                                    fastInitialize(
                                                        chipDate,
                                                        items.toTypedArray(),
                                                        choice
                                                    )
                                                    setOnItemClickListener { _, _, position, _ ->
                                                        dismiss()
                                                        mediaInfo.restoreIndex = position
                                                        text =
                                                            Command.getDate(mediaInfo.detailRestoreList[mediaInfo.restoreIndex].date)
                                                    }
                                                    show()
                                                }
                                            }
                                        }
                                    }
                                    materialButtonConfirm.setOnClickListener {
                                        viewModel.viewModelScope.launch {
                                            dismiss()
                                            GsonUtil.saveMediaInfoRestoreMapToFile(viewModel.globalObject.mediaInfoRestoreMap.value)
                                        }

                                    }
                                    materialButtonRemove.setOnClickListener {
                                        MaterialAlertDialogBuilder(requireContext()).apply {
                                            setWithConfirm(
                                                "${GlobalString.confirmRemove}${GlobalString.symbolQuestion}\n" +
                                                        "${GlobalString.removeFilesToo}${GlobalString.symbolExclamation}"
                                            ) {
                                                viewModel.viewModelScope.launch {
                                                    Command.rm("${Path.getBackupMediaSavePath()}/${mediaInfo.name}/${mediaInfo.detailRestoreList[mediaInfo.restoreIndex].date}")
                                                        .apply {
                                                            if (this) {
                                                                mediaInfo.detailRestoreList.remove(
                                                                    mediaInfo.detailRestoreList[mediaInfo.restoreIndex]
                                                                )
                                                                mediaInfo.restoreIndex--
                                                                dismiss()
                                                                viewModel.isInitialized = false
                                                                GsonUtil.saveMediaInfoRestoreMapToFile(
                                                                    viewModel.globalObject.mediaInfoRestoreMap.value
                                                                )
                                                                Toast.makeText(
                                                                    context,
                                                                    GlobalString.success,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                Toast.makeText(
                                                                    requireContext(),
                                                                    "${GlobalString.removeFailed}${GlobalString.symbolExclamation}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }.root
                            )
                        }
                        setCancelable(false)
                        setCanceledOnTouchOutside(false)
                    }
                    false
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