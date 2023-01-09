package com.xayah.databackup.fragment.cloud

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.xayah.databackup.App
import com.xayah.databackup.data.RcloneConfig
import com.xayah.databackup.databinding.FragmentCloudBinding
import com.xayah.databackup.util.ExtendCommand
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.readRcloneConfigName
import com.xayah.databackup.util.saveRcloneConfigName
import com.xayah.databackup.view.InputChip
import com.xayah.databackup.view.setWithTopBar
import com.xayah.databackup.view.title
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer


class CloudFragment : Fragment() {
    private var _binding: FragmentCloudBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CloudViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCloudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CloudViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // 初始化文件浏览器
        val that = this
        viewModel.materialYouFileExplorer = MaterialYouFileExplorer().apply {
            initialize(that)
        }

        // 观察isRefreshed Flow
        viewModel.runOnScope {
            viewModel.isRefreshed.collect {
                if (it.not()) {
                    viewModel.initialize()
                    setChipGroup(binding.chipGroup)
                    viewModel.refresh(true)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 读取Rclone配置列表并设置ChipGroup
     */
    private fun setChipGroup(chipGroup: ChipGroup) {
        viewModel.runOnScope {
            // 读取并提交Rclone本地配置数据
            viewModel.rcloneConfigList.emit(ExtendCommand.rcloneConfigParse())
            // 移除所有子Chip
            chipGroup.removeAllViews()
            for (i in viewModel.rcloneConfigList.value) {
                binding.chipGroup.addView(setChip(i))
            }
        }
    }

    /**
     * 设置Rclone配置Chip
     */
    private fun setChip(rcloneConfig: RcloneConfig): Chip {
        return InputChip(layoutInflater, binding.chipGroup).apply {
            text = rcloneConfig.name
            isChecked = rcloneConfig.name == App.globalContext.readRcloneConfigName()
            isCloseIconVisible = false
            setOnCheckedChangeListener { _, isChecked ->
                // 单击选中
                viewModel.runOnScope {
                    if (isChecked)
                        App.globalContext.saveRcloneConfigName(rcloneConfig.name)
                }
            }
            setOnLongClickListener {
                // 长按弹出配置BottomSheet
                BottomSheetDialog(context).apply {
                    val binding =
                        viewModel.commonBottomSheetRcloneConfigDetailBinding(context) { dismiss() }
                            .apply {
                                // 读取配置
                                textInputEditTextName.setText(rcloneConfig.name)
                                textInputEditTextServerAddress.setText(rcloneConfig.url)
                                textInputEditTextUsername.setText(rcloneConfig.user)
                                textInputEditTextPassword.setText(rcloneConfig.pass)

                                // 移除按钮点击事件
                                materialButtonRemove.setOnClickListener {
                                    viewModel.runOnScope {
                                        ExtendCommand.rcloneConfigDelete(rcloneConfig.name)
                                        dismiss()
                                        viewModel.refresh(false)
                                    }
                                }
                            }
                    setWithTopBar().apply {
                        addView(title(GlobalString.configuration))
                        addView(binding.root)
                    }
                }
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 恢复时刷新
        viewModel.refresh(false)
    }
}