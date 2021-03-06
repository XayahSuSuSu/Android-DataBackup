package com.xayah.databackup.fragment.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.ShellUtils
import com.xayah.databackup.App
import com.xayah.databackup.activity.guide.GuideActivity
import com.xayah.databackup.databinding.FragmentGuideTwoBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class GuideTwoFragment : Fragment() {
    private var _binding: FragmentGuideTwoBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GuideTwoViewModel
    private lateinit var hostActivity: GuideActivity
    private lateinit var versionName: String
    private var step = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideTwoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[GuideTwoViewModel::class.java]
        binding.viewModel = viewModel

        hostActivity = requireActivity() as GuideActivity
        hostActivity.setBtnOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                hostActivity.setBtnEnabled(false)
                when (step) {
                    0 -> {
                        rootAccess()
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
                hostActivity.setBtnEnabled(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun rootAccess() {
        val isRoot = Command.checkRoot()
        if (isRoot) {
            Command.mkdir(Path.getExternalStorageDataBackupDirectory()).apply {
                if (this) {
                    viewModel.grantRootAccessCheck.set(GlobalString.symbolTick)
                    hostActivity.setBtnText(GlobalString.releasePrebuiltBinaries)
                    step++
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            requireContext(), GlobalString.backupDirCreateFailed, Toast.LENGTH_SHORT
                        ).show()
                    }
                    viewModel.grantRootAccessCheck.set(GlobalString.symbolCross)
                }
            }
        } else {
            viewModel.grantRootAccessCheck.set(GlobalString.symbolCross)
        }
    }

    private fun binRelease() {
        versionName = App.versionName
        val oldVersionName = ShellUtils.fastCmd("cat ${Path.getFilesDir(hostActivity)}/version")
        if (versionName > oldVersionName) {
            ShellUtils.fastCmd("rm -rf ${Path.getFilesDir(hostActivity)}/bin")
            ShellUtils.fastCmd("rm -rf ${Path.getFilesDir(hostActivity)}/bin.zip")
        }

        if (!Command.ls("${Path.getFilesDir(hostActivity)}/bin")) {
            Command.extractAssets(
                hostActivity, "${Command.getABI()}/bin.zip", "bin.zip"
            )
            Command.unzipByZip4j(
                "${Path.getFilesDir(hostActivity)}/bin.zip", "${Path.getFilesDir(hostActivity)}/bin"
            )
            Bashrc.writeToFile(versionName, "${Path.getFilesDir(hostActivity)}/version")
        }
        Command.execute("chmod 777 -R ${Path.getFilesDir(hostActivity)}")
        Command.checkBin(App.globalContext).apply {
            if (this) {
                viewModel.releasePrebuiltBinariesCheck.set(GlobalString.symbolTick)
                hostActivity.setBtnText(GlobalString.activateBashrc)
                step++
            } else {
                viewModel.releasePrebuiltBinariesCheck.set(GlobalString.symbolCross)
                CoroutineScope(Dispatchers.Main).launch {
                    MaterialAlertDialogBuilder(hostActivity).apply {
                        setWithConfirm(
                            "${Path.getFilesDir(hostActivity)}/bin: ${GlobalString.binPermissionError}",
                            cancelable = false,
                            hasNegativeBtn = false
                        ) {}
                    }
                }
            }
        }
    }

    private fun checkBashrc() {
        Command.execute("check_bashrc").isSuccess.apply {
            if (this) {
                viewModel.activateBashrcCheck.set(GlobalString.symbolTick)
                hostActivity.setBtnText(GlobalString.finish)
                step++
            } else {
                viewModel.activateBashrcCheck.set(GlobalString.symbolCross)
            }
        }
    }

    private fun finish() {
        hostActivity.saveInitializedVersionName(versionName)
        App.initializeGlobalList()
        hostActivity.toMainActivity()
    }
}