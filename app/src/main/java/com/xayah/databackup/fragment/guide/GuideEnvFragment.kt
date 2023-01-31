package com.xayah.databackup.fragment.guide

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.permissionx.guolindev.PermissionX
import com.xayah.databackup.App
import com.xayah.databackup.activity.guide.GuideViewModel
import com.xayah.databackup.databinding.FragmentGuideEnvBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


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
        binding.lifecycleOwner = requireActivity()

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
                            checkStorageManagementPermission()
                        }
                        4 -> {
                            checkPackageUsageStatsPermission()
                        }
                        5 -> {
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
        val isRoot = Command.checkRoot()
        if (isRoot) {
            viewModel.grantRootAccessCheck.set(GlobalString.success)
            step++
            return
        }
        viewModel.grantRootAccessCheck.set(GlobalString.failed)
    }

    private suspend fun binRelease() {
        // 环境目录
        val filesDirectory = Path.getAppInternalFilesPath()
        val binPath = "${filesDirectory}/bin"
        val binZipPath = "${filesDirectory}/bin.zip"

        val bin = File(binPath)
        // 环境检测与释放
        withContext(Dispatchers.IO) {
            val oldVersionName = requireContext().readAppVersion()
            if (App.versionName > oldVersionName) {
                bin.deleteRecursively()
            }
            val isBinReleased = bin.exists()
            if (!isBinReleased) {
                Command.releaseAssets(
                    requireContext(), "bin/${Command.getABI()}/bin.zip", "bin.zip"
                )
                Command.unzipByZip4j(binZipPath, binPath)
                requireContext().saveAppVersion(App.versionName)
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

    private fun checkStorageManagementPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            PermissionX.init(this).permissions(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            ).request { allGranted, _, _ ->
                if (!allGranted) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    startActivity(intent)
                } else {
                    viewModel.storageManagementPermissionCheck.set(GlobalString.success)
                    step++
                }
            }
        } else {
            PermissionX.init(this).permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).request { allGranted, _, _ ->
                if (allGranted) {
                    viewModel.storageManagementPermissionCheck.set(GlobalString.success)
                    step++
                }
            }
        }
    }

    private fun checkPackageUsageStatsPermission() {
        if (App.globalContext.checkPackageUsageStatsPermission()) {
            // 已获取权限
            viewModel.packageUsageStatsPermissionCheck.set(GlobalString.success)
            guideViewModel.btnNextText.postValue(GlobalString.finish)
            App.globalContext.saveIsSupportUsageAccess(true)
            step++
        } else {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                intent.data = Uri.parse("package:${App.globalContext.packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                guideViewModel.btnNextText.postValue(GlobalString.finish)
                App.globalContext.saveIsSupportUsageAccess(false)
                step++
                e.printStackTrace()
            }
        }
    }

    private fun finish() {
        viewModel.viewModelScope.launch {
            App.globalContext.saveInitializedVersionName(App.versionName)
            guideViewModel.finishAndEnter.postValue(true)
        }
    }
}