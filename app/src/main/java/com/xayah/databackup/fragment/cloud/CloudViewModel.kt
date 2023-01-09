package com.xayah.databackup.fragment.cloud

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.xayah.databackup.App
import com.xayah.databackup.data.RcloneConfig
import com.xayah.databackup.databinding.BottomSheetRcloneConfigDetailBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.ExtendCommand
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.setWithTopBar
import com.xayah.databackup.view.title
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CloudViewModel : ViewModel() {
    // 文件浏览器
    lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    // 是否已刷新, 用于观察者监听数据
    val isRefreshed by lazy {
        MutableStateFlow(false)
    }

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

    // 配置列表
    val rcloneConfigList by lazy {
        MutableStateFlow(mutableListOf<RcloneConfig>())
    }

    /**
     * 切换至ViewModelScope协程运行
     */
    fun <T> runOnScope(block: suspend () -> T) {
        viewModelScope.launch { block() }
    }

    /**
     * 提交刷新Flow
     */
    fun refresh(value: Boolean) {
        runOnScope { isRefreshed.emit(value) }
    }

    /**
     * 初始化上方Rclone环境信息
     */
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

    /**
     * Rclone环境安装按钮点击事件
     */
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

    /**
     * Rclone环境本地导入点击事件
     */
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

    /**
     * 解压/安装扩展模块
     */
    private suspend fun installExtendModule(savePath: String) {
        Command.unzipByZip4j(
            savePath,
            "${Path.getFilesDir()}/extend"
        )
        Command.execute("chmod 777 -R ${Path.getFilesDir()}")
        initialize()
    }

    /**
     * 检验合法性, 为空则不合法
     */
    private fun emptyTextValidation(
        textInputEditText: TextInputEditText,
        textInputLayout: TextInputLayout,
        errorString: String
    ): Boolean {
        return if (textInputEditText.text.toString().trim().isEmpty()) {
            textInputLayout.isErrorEnabled = true
            textInputLayout.error = errorString
            false
        } else {
            textInputLayout.isErrorEnabled = false
            textInputLayout.error = ""
            true
        }
    }

    /**
     * 通用BottomSheetRcloneConfigDetailBinding配置
     */
    fun commonBottomSheetRcloneConfigDetailBinding(
        context: Context,
        onConfirm: () -> Unit
    ): BottomSheetRcloneConfigDetailBinding {
        return BottomSheetRcloneConfigDetailBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            // 监听名称输入
            textInputEditTextName.addTextChangedListener {
                emptyTextValidation(
                    textInputEditTextName,
                    textInputLayoutName,
                    GlobalString.nameEmptyError
                )
            }

            // 监听服务器地址输入
            textInputEditTextServerAddress.addTextChangedListener {
                emptyTextValidation(
                    textInputEditTextServerAddress,
                    textInputLayoutServerAddress,
                    GlobalString.serverAddressEmptyError
                )
            }

            // 确定按钮点击事件
            materialButtonConfirm.setOnClickListener {
                val name = textInputEditTextName.text.toString().trim()
                val url = textInputEditTextServerAddress.text.toString().trim()
                val user = textInputEditTextUsername.text.toString().trim()
                val pass = textInputEditTextPassword.text.toString()

                // 名称合法性验证
                val nameValidation = emptyTextValidation(
                    textInputEditTextName,
                    textInputLayoutName,
                    GlobalString.nameEmptyError
                )

                // 服务器地址合法性验证
                val serverAddressValidation = emptyTextValidation(
                    textInputEditTextServerAddress,
                    textInputLayoutServerAddress,
                    GlobalString.serverAddressEmptyError
                )

                val isValid = nameValidation && serverAddressValidation
                if (isValid)
                    runOnScope {
                        // 创建/修改配置
                        ExtendCommand.rcloneConfigCreate(name, url, user, pass)
                        // 刷新
                        refresh(false)
                        onConfirm()
                    }
            }
        }
    }

    /**
     * Rclone配置添加按钮点击事件
     */
    fun onConfigAddBtnClick(v: View) {
        BottomSheetDialog(v.context).apply {
            val binding =
                commonBottomSheetRcloneConfigDetailBinding(v.context) { dismiss() }.apply {
                    materialButtonRemove.isEnabled = false
                }
            setWithTopBar().apply {
                addView(title(GlobalString.configuration))
                addView(binding.root)
            }
        }
    }
}