package com.xayah.databackup.fragment.cloud

import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.ExtendCommand
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.launch

class CloudViewModel : ViewModel() {
    lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    // 扩展是否已安装
    var isReady: ObservableField<Boolean> = ObservableField(true)

    // 是否正在安装, 决定安装交互是否显示
    var isInstalling: ObservableField<Boolean> = ObservableField(false)

    // 安装交互进度是否循环
    var isIndeterminate: ObservableField<Boolean> = ObservableField(true)

    // 安装交互进度
    var installProgress: ObservableField<Int> = ObservableField(0)

    // 安装交互提示
    var installState: ObservableField<String> = ObservableField(GlobalString.symbolQuestion)

    // 扩展版本
    var rcloneVersion: ObservableField<String> = ObservableField(GlobalString.symbolQuestion)
    var fusermountVersion: ObservableField<String> = ObservableField(GlobalString.symbolQuestion)

    // Fuse状态
    var fuseState: ObservableField<String> = ObservableField(GlobalString.symbolQuestion)

    fun initialize() {
        isInstalling.set(false)
        viewModelScope.launch {
            ExtendCommand.checkExtend().apply {
                isReady.set(this)
            }
            rcloneVersion.set(ExtendCommand.checkRcloneVersion())
            fusermountVersion.set(ExtendCommand.checkFusermountVersion())
            fuseState.set(if (Command.ls(GlobalString.devFuse)) GlobalString.symbolTick else GlobalString.symbolCross)
        }
    }

    fun onInstallOnlineBtnClick(v: View) {
        isInstalling.set(true)
        installState.set(GlobalString.prepareForDownloading)
        installProgress.set(0)
        val savePath = "${Path.getFilesDir()}/extend.zip"

        viewModelScope.launch {
            App.server.releases({ releaseList ->
                val mReleaseList = releaseList.filter { it.name.contains("Extend") }
                if (mReleaseList.isEmpty()) {
                    isInstalling.set(false)
                } else {
                    for (i in mReleaseList.first().assets) {
                        if (i.browser_download_url.contains(Command.getABI())) {
                            viewModelScope.launch {
                                App.server.download(
                                    url = i.browser_download_url,
                                    savePath = savePath,
                                    onDownload = { current, total ->
                                        viewModelScope.launch {
                                            isIndeterminate.set(false)
                                            installProgress.set((current.toFloat() / total * 100).toInt())
                                            installState.set("${(current.toFloat() / total * 100).toInt()}%")
                                        }
                                    },
                                    onSuccess = {
                                        viewModelScope.launch {
                                            isIndeterminate.set(true)
                                            installExtendModule(savePath)
                                        }
                                    },
                                    onFailed = {
                                        isInstalling.set(false)
                                    })
                            }
                            break
                        }
                    }
                }
            }, { isInstalling.set(false) })
        }
    }

    fun onImportLocallyBtnClick(v: View) {
        val savePath = "${Path.getFilesDir()}/extend.zip"
        materialYouFileExplorer.apply {
            isFile = true
            toExplorer(v.context) { path, _ ->
                Toast.makeText(v.context, path, Toast.LENGTH_SHORT).show()
                viewModelScope.launch {
                    Command.cp(path, savePath)
                    installExtendModule(savePath)
                }
            }
        }
    }

    private suspend fun installExtendModule(savePath: String) {
        Command.unzipByZip4j(
            savePath,
            "${Path.getFilesDir()}/extend"
        )
        Command.execute("chmod 777 -R ${Path.getFilesDir()}")
        initialize()
    }
}