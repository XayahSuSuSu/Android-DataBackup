package com.xayah.databackup.fragment.guide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.activity.guide.GuideViewModel
import com.xayah.databackup.databinding.FragmentGuideEnvBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GuideEnvFragment : Fragment() {
    private var _binding: FragmentGuideEnvBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GuideEnvViewModel
    private lateinit var guideViewModel: GuideViewModel
    private var step = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideEnvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        guideViewModel = ViewModelProvider(requireActivity())[GuideViewModel::class.java]
        viewModel = ViewModelProvider(this)[GuideEnvViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // 标题
        guideViewModel.title.set(GlobalString.environmentDetection)

        guideViewModel.apply {
            btnNextOnClick.postValue {
                viewModel.viewModelScope.launch {
                    guideViewModel.btnEnabled.postValue(false)
                    when (step) {
                        0 -> {
                            checkRootAccess()
                        }
                        1 -> {
                            binRelease()
                        }
                        2 -> {
                            checkBashrc()
                        }
                        3 -> {
                            checkPackageUsageStatsPermission()
                        }
                        4 -> {
                            finish()
                        }
                    }
                    guideViewModel.btnEnabled.postValue(true)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun checkRootAccess() {
        val isRoot = withContext(Dispatchers.IO) {
            Command.checkRoot()
        }
        if (isRoot) {
            viewModel.grantRootAccessCheck.set(GlobalString.success)
            step++
            return
        }
        viewModel.grantRootAccessCheck.set(GlobalString.failed)
    }

    private suspend fun binRelease() {
        // 环境目录
        val filesDirectory = Path.getFilesDir()
        val versionPath = "${filesDirectory}/version"
        val binPath = "${filesDirectory}/bin"
        val binZipPath = "${filesDirectory}/bin.zip"
        // 环境检测与释放
        withContext(Dispatchers.IO) {
            val oldVersionName = Command.execute("cat \"${versionPath}\"").out.joinToLineString
            if (App.versionName > oldVersionName) {
                Command.execute("rm -rf \"${binPath}\" \"${binZipPath}\"")
            }
            val isBinReleased = Command.ls(binPath)
            if (!isBinReleased) {
                Command.releaseAssets(
                    requireContext(), "bin/${Command.getABI()}/bin.zip", "bin.zip"
                )
                Command.unzipByZip4j(binZipPath, binPath)
                Bashrc.writeToFile(App.versionName, versionPath)
            }
            Command.execute("chmod 777 -R \"${filesDirectory}\"")
            Command.checkBin().apply {
                val that = this
                withContext(Dispatchers.Main) {
                    if (that) {
                        viewModel.releasePrebuiltBinariesCheck.set(GlobalString.success)
                        step++
                    } else {
                        viewModel.releasePrebuiltBinariesCheck.set(GlobalString.failed)
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setWithConfirm(
                                "${binPath}: ${GlobalString.binPermissionError}",
                                cancelable = false,
                                hasNegativeBtn = false
                            ) {}
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkBashrc() {
        val bashrcTest = withContext(Dispatchers.IO) {
            Command.execute("check_bashrc").isSuccess
        }
        if (bashrcTest) {
            viewModel.activateBashrcCheck.set(GlobalString.success)
            step++
        } else {
            viewModel.activateBashrcCheck.set(GlobalString.failed)
        }
    }

    private fun checkPackageUsageStatsPermission() {
        if (App.globalContext.checkPackageUsageStatsPermission()) {
            // 已获取权限
            viewModel.packageUsageStatsPermissionCheck.set(GlobalString.success)
            guideViewModel.btnNextText.postValue(GlobalString.finish)
            step++
        } else {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.data = Uri.parse("package:${App.globalContext.packageName}")
            startActivity(intent)
        }
    }

    private fun finish() {
        viewModel.viewModelScope.launch {
            App.globalContext.saveInitializedVersionName(App.versionName)
            guideViewModel.finishAndEnter.postValue(true)
        }
    }
}