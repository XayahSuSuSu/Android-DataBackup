package com.xayah.databackup.fragment.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.activity.guide.GuideViewModel
import com.xayah.databackup.databinding.FragmentGuideTwoBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GuideTwoFragment : Fragment() {
    private var _binding: FragmentGuideTwoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GuideTwoViewModel
    private lateinit var guideViewModel: GuideViewModel
    private var step = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideTwoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        guideViewModel = ViewModelProvider(requireActivity())[GuideViewModel::class.java]
        viewModel = ViewModelProvider(this)[GuideTwoViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        guideViewModel.apply {
            btnNextOnClick.postValue {
                viewModel.viewModelScope.launch {
                    guideViewModel.btnEnabled.postValue(false)
                    when (step) {
                        0 -> {
                            onRootAccess()
                        }
                        1 -> {
                            binRelease()
                        }
                        2 -> {
                            checkBashrc()
                        }
                        3 -> {
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

    private suspend fun onRootAccess() {
        val isRoot = withContext(Dispatchers.IO) {
            Command.checkRoot()
        }
        if (isRoot) {
            viewModel.grantRootAccessCheck.postValue(GlobalString.symbolTick)
            guideViewModel.btnNextText.postValue(GlobalString.releasePrebuiltBinaries)
            step++
            return
        }
        viewModel.grantRootAccessCheck.postValue(GlobalString.symbolCross)
    }

    private suspend fun binRelease() {
        // 环境目录
        val filesDirectory = Path.getFilesDir()
        val versionPath = "${filesDirectory}/version"
        val binPath = "${filesDirectory}/bin"
        val binZipPath = "${filesDirectory}/bin.zip"
        // 环境检测与释放
        withContext(Dispatchers.IO) {
            val oldVersionName = Command.execute("cat $versionPath").out.joinToLineString
            if (App.versionName > oldVersionName) {
                Command.execute("rm -rf $binPath $binZipPath")
            }
            val isBinReleased = Command.ls(binPath)
            if (!isBinReleased) {
                Command.releaseAssets(
                    requireContext(), "bin/${Command.getABI()}/bin.zip", "bin.zip"
                )
                Command.unzipByZip4j(binZipPath, binPath)
                Bashrc.writeToFile(App.versionName, versionPath)
            }
            Command.execute("chmod 777 -R $filesDirectory")
            Command.checkBin().apply {
                val that = this
                withContext(Dispatchers.Main) {
                    if (that) {
                        viewModel.releasePrebuiltBinariesCheck.postValue(GlobalString.symbolTick)
                        guideViewModel.btnNextText.postValue(GlobalString.activateBashrc)
                        step++
                    } else {
                        viewModel.releasePrebuiltBinariesCheck.postValue(GlobalString.symbolCross)
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
            viewModel.activateBashrcCheck.postValue(GlobalString.symbolTick)
            guideViewModel.btnNextText.postValue(GlobalString.finish)
            step++
        } else {
            viewModel.activateBashrcCheck.postValue(GlobalString.symbolCross)
        }
    }

    private fun finish() {
        viewModel.viewModelScope.launch {
            App.globalContext.saveInitializedVersionName(App.versionName)
            guideViewModel.finishAndEnter.postValue(true)
        }
    }
}