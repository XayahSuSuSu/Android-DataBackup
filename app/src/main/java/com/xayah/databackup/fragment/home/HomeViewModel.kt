package com.xayah.databackup.fragment.home

import android.content.Context
import android.view.View
import androidx.core.view.size
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.xayah.databackup.MainActivity
import com.xayah.databackup.R
import com.xayah.databackup.util.Bashrc
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.Path
import com.xayah.design.view.setWithTopBarAndTips

class HomeViewModel : ViewModel() {
    val isRoot by lazy {
        SuFile.open("/dev/console").canRead()
    }
    lateinit var abi: String
    lateinit var storageSpace: String
    var initialized = false

    var isEnvCorrect = true
    var envList = mutableListOf<String>()

    fun initialize(context: Context) {
        if (!initialized) {
            abi = Command.getABI()
            storageSpace =
                if (Bashrc.getStorageSpace().first) Bashrc.getStorageSpace().second else context.getString(
                    R.string.error
                )

            envList.clear()
            envList.add(context.getString(R.string.environment_detection_tip))
            Shell.cmd("which --help").exec().isSuccess.apply {
                if (this)
                    envList.add("which: √")
                else {
                    envList.add("which: ×")
                    isEnvCorrect = false
                }
            }
            Shell.cmd("which unzip").exec().isSuccess.apply {
                if (this)
                    envList.add("unzip: √")
                else {
                    envList.add("unzip: ×")
                    isEnvCorrect = false
                }
            }
            Shell.cmd("ls ${Path.getFilesDir(context)}/bin").exec().out.apply {
                if (this.size == 3)
                    envList.add("bin: √")
                else {
                    envList.add("bin: ×")
                    isEnvCorrect = false
                }
            }
            Shell.cmd("check_bashrc").exec().isSuccess.apply {
                if (this)
                    envList.add("bashrc: √")
                else {
                    envList.add("bashrc: ×")
                    isEnvCorrect = false
                }
            }
            if (!isEnvCorrect) {
                for (i in 0 until (context as MainActivity).binding.bottomNavigation.menu.size) {
                    context.binding.bottomNavigation.menu.getItem(i).isEnabled = false
                }
                showBottomSheetDialog(context)
            }
            initialized = true
        }
    }

    fun showEnv(v: View) {
        showBottomSheetDialog(v.context)
    }

    private fun showBottomSheetDialog(context: Context) {
        BottomSheetDialog(context).apply {
            setWithTopBarAndTips(
                context.getString(R.string.environment_detection),
                envList.joinToString(separator = "\n"),
                R.raw.crown
            ) {}
        }
    }
}