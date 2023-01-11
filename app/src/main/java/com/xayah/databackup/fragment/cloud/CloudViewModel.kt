package com.xayah.databackup.fragment.cloud

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.widget.addTextChangedListener
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.RcloneConfig
import com.xayah.databackup.data.RcloneMount
import com.xayah.databackup.databinding.BottomSheetRcloneConfigDetailBinding
import com.xayah.databackup.databinding.BottomSheetRcloneConfigDetailFtpBinding
import com.xayah.databackup.databinding.BottomSheetRcloneConfigDetailWebdavBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.fastInitialize
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
    var hasUpdate: ObservableField<Boolean> = ObservableField(false)

    // Fuse状态
    var fuseState: ObservableField<String> = ObservableField(GlobalString.symbolQuestion)

    // 挂载信息
    var mountState: ObservableField<String> = ObservableField(GlobalString.notMounted)
    var mountName: ObservableField<String> = ObservableField(GlobalString.notSelected)
    var mountDest: ObservableField<String> = ObservableField(GlobalString.notSelected)
    var mountIcon: ObservableField<Drawable> =
        ObservableField(App.globalContext.getDrawable(R.drawable.ic_outline_light))
    var isChangingMount: ObservableField<Boolean> = ObservableField(false)

    // 配置列表
    val rcloneConfigList by lazy {
        MutableStateFlow(mutableListOf<RcloneConfig>())
    }

    // 挂载哈希表
    private val rcloneMountMap by lazy {
        MutableStateFlow(hashMapOf<String, RcloneMount>())
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
     * 初始化上方Rclone环境信息和挂载哈希表
     */
    fun initialize() {
        isInstalling.set(false)
        runOnScope {
            App.server.releases({ releaseList ->
                runOnScope {
                    val mReleaseList = releaseList.filter { it.name.contains("Extend") }
                    if (mReleaseList.isNotEmpty()) {
                        // 检查是否有更新
                        hasUpdate.set(
                            ExtendCommand.checkExtendLocalVersion() != mReleaseList.first().name.replace(
                                "Extend-",
                                ""
                            )
                        )
                    }
                }
            }, { })
            ExtendCommand.checkExtend().apply {
                isReady.set(this)
            }
            rcloneVersion.set(ExtendCommand.checkRcloneVersion())
            fusermountVersion.set(ExtendCommand.checkFusermountVersion())
            fuseState.set(if (Command.ls(GlobalString.devFuse)) GlobalString.symbolTick else GlobalString.symbolCross)
            rcloneMountMap.emit(ExtendCommand.getRcloneMountMap())
            if (rcloneConfigList.value.isNotEmpty() && mountName.get() == GlobalString.notSelected) {
                // 默认显示第一个配置
                changeMount(rcloneConfigList.value.first())
            }
        }
    }

    /**
     * Rclone环境安装按钮点击事件
     */
    fun onInstallOnlineBtnClick(v: View) {
        isReady.set(false)
        isInstalling.set(true)
        installState.set(GlobalString.prepareForDownloading)
        installProgress.set(0)
        val savePath = "${Path.getFilesDir()}/extend.zip"

        runOnScope {
            App.server.releases(successCallback = { releaseList ->
                val mReleaseList = releaseList.filter { it.name.contains("Extend") }
                if (mReleaseList.isEmpty()) {
                    isInstalling.set(false)
                } else {
                    for (i in mReleaseList.first().assets) {
                        if (i.browser_download_url.contains(Command.getABI())) {
                            runOnScope {
                                App.server.download(
                                    url = i.browser_download_url,
                                    savePath = savePath,
                                    onDownload = { current, total ->
                                        runOnScope {
                                            isIndeterminate.set(false)
                                            installProgress.set((current.toFloat() / total * 100).toInt())
                                            installState.set("${(current.toFloat() / total * 100).toInt()}%")
                                        }
                                    },
                                    onSuccess = {
                                        runOnScope {
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
            }, failedCallback = {
                isInstalling.set(false)
            })
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
                runOnScope {
                    Command.cp(path, savePath)
                    Command.execute("chmod 777 -R ${savePath}")
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
        onConfirm: () -> Unit,
        rcloneConfig: RcloneConfig? = null,
        onRemove: () -> Unit = {}
    ): BottomSheetRcloneConfigDetailBinding {
        return BottomSheetRcloneConfigDetailBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        ).apply {
            val typeKeyList =
                App.globalContext.resources.getStringArray(R.array.rclone_config_type_array_key)
            val typeValueList =
                App.globalContext.resources.getStringArray(R.array.rclone_config_type_array_value)

            autoCompleteTextViewType.setText(typeKeyList[0], false)
            rcloneConfig?.apply {
                val index = typeValueList.indexOf(type)
                if (index != -1) {
                    autoCompleteTextViewType.setText(typeKeyList[index], false)
                    autoCompleteTextViewType.isEnabled = false
                }
            }

            val judgeType = {
                content.removeAllViews()
                when (autoCompleteTextViewType.text.toString().trim()) {
                    // WebDAV
                    typeKeyList[0] -> {
                        val binding = BottomSheetRcloneConfigDetailWebdavBinding.inflate(
                            LayoutInflater.from(context),
                            null,
                            false
                        ).apply {
                            // 监听名称输入
                            textInputEditTextName.addTextChangedListener {
                                emptyTextValidation(
                                    textInputEditTextName,
                                    textInputLayoutName,
                                    GlobalString.emptyError
                                )
                            }

                            // 监听服务器地址输入
                            textInputEditTextServerAddress.addTextChangedListener {
                                emptyTextValidation(
                                    textInputEditTextServerAddress,
                                    textInputLayoutServerAddress,
                                    GlobalString.emptyError
                                )
                            }

                            if (rcloneConfig == null) {
                                materialButtonRemove.isEnabled = false
                            } else {
                                // 读取配置
                                textInputEditTextName.setText(rcloneConfig.name)
                                textInputEditTextName.isEnabled = false
                                textInputEditTextServerAddress.setText(rcloneConfig.url)
                                textInputEditTextUsername.setText(rcloneConfig.user)
                                textInputEditTextPassword.setText(rcloneConfig.pass)

                                // 移除按钮点击事件
                                materialButtonRemove.setOnClickListener {
                                    runOnScope {
                                        ExtendCommand.rcloneConfigDelete(rcloneConfig.name)
                                        onRemove()
                                    }
                                }
                            }
                        }
                        // 确定按钮点击事件
                        materialButtonConfirm.setOnClickListener {
                            val type = typeValueList[typeKeyList.indexOf(
                                autoCompleteTextViewType.text.toString().trim()
                            )]
                            val name = binding.textInputEditTextName.text.toString().trim()
                            val url = binding.textInputEditTextServerAddress.text.toString().trim()
                            val user = binding.textInputEditTextUsername.text.toString().trim()
                            val pass = binding.textInputEditTextPassword.text.toString()

                            // 名称合法性验证
                            val nameValidation = emptyTextValidation(
                                binding.textInputEditTextName,
                                binding.textInputLayoutName,
                                GlobalString.emptyError
                            )

                            // 服务器地址合法性验证
                            val serverAddressValidation = emptyTextValidation(
                                binding.textInputEditTextServerAddress,
                                binding.textInputLayoutServerAddress,
                                GlobalString.emptyError
                            )

                            val isValid = nameValidation && serverAddressValidation
                            if (isValid)
                                runOnScope {
                                    // 创建/修改配置
                                    val args =
                                        "url=\"${url}\" vendor=other user=\"${user}\" pass=\"${pass}\""
                                    ExtendCommand.rcloneConfigCreate(type, name, args)
                                    // 刷新
                                    refresh(false)
                                    onConfirm()
                                }
                        }
                        content.addView(binding.root)
                    }
                    // FTP
                    typeKeyList[1] -> {
                        val binding = BottomSheetRcloneConfigDetailFtpBinding.inflate(
                            LayoutInflater.from(context),
                            null,
                            false
                        ).apply {
                            // 监听名称输入
                            textInputEditTextName.addTextChangedListener {
                                emptyTextValidation(
                                    textInputEditTextName,
                                    textInputLayoutName,
                                    GlobalString.emptyError
                                )
                            }

                            // 监听服务器地址输入
                            textInputEditTextServerAddress.addTextChangedListener {
                                emptyTextValidation(
                                    textInputEditTextServerAddress,
                                    textInputLayoutServerAddress,
                                    GlobalString.emptyError
                                )
                            }

                            // 监听端口输入
                            textInputEditTextPort.addTextChangedListener {
                                emptyTextValidation(
                                    textInputEditTextPort,
                                    textInputLayoutPort,
                                    GlobalString.emptyError
                                )
                            }

                            if (rcloneConfig == null) {
                                materialButtonRemove.isEnabled = false
                            } else {
                                // 读取配置
                                textInputEditTextName.setText(rcloneConfig.name)
                                textInputEditTextName.isEnabled = false
                                textInputEditTextServerAddress.setText(rcloneConfig.host)
                                textInputEditTextPort.setText(rcloneConfig.port)
                                textInputEditTextUsername.setText(rcloneConfig.user)
                                textInputEditTextPassword.setText(rcloneConfig.pass)

                                // 移除按钮点击事件
                                materialButtonRemove.setOnClickListener {
                                    runOnScope {
                                        ExtendCommand.rcloneConfigDelete(rcloneConfig.name)
                                        onRemove()
                                    }
                                }
                            }
                        }
                        // 确定按钮点击事件
                        materialButtonConfirm.setOnClickListener {
                            val type = typeValueList[typeKeyList.indexOf(
                                autoCompleteTextViewType.text.toString().trim()
                            )]
                            val name = binding.textInputEditTextName.text.toString().trim()
                            val host = binding.textInputEditTextServerAddress.text.toString().trim()
                            val port = binding.textInputEditTextPort.text.toString().trim()
                            val user = binding.textInputEditTextUsername.text.toString().trim()
                            val pass = binding.textInputEditTextPassword.text.toString()

                            // 名称合法性验证
                            val nameValidation = emptyTextValidation(
                                binding.textInputEditTextName,
                                binding.textInputLayoutName,
                                GlobalString.emptyError
                            )

                            // 服务器地址合法性验证
                            val serverAddressValidation = emptyTextValidation(
                                binding.textInputEditTextServerAddress,
                                binding.textInputLayoutServerAddress,
                                GlobalString.emptyError
                            )

                            // 端口合法性验证
                            val portValidation = emptyTextValidation(
                                binding.textInputEditTextPort,
                                binding.textInputLayoutPort,
                                GlobalString.emptyError
                            )

                            val isValid =
                                nameValidation && serverAddressValidation && portValidation
                            if (isValid)
                                runOnScope {
                                    // 创建/修改配置
                                    val args =
                                        "host=\"${host}\" port=\"${port}\" user=\"${user}\" pass=\"${pass}\""
                                    ExtendCommand.rcloneConfigCreate(type, name, args)
                                    // 刷新
                                    refresh(false)
                                    onConfirm()
                                }
                        }
                        content.addView(binding.root)
                    }
                }
            }

            autoCompleteTextViewType.setOnItemClickListener { _, _, i, _ ->
                autoCompleteTextViewType.setText(typeKeyList[i], false)
                judgeType()
            }

            judgeType()
        }
    }

    /**
     * Rclone配置添加按钮点击事件
     */
    fun onConfigAddBtnClick(v: View) {
        BottomSheetDialog(v.context).apply {
            val binding =
                commonBottomSheetRcloneConfigDetailBinding(v.context, { dismiss() }, null)
            setWithTopBar().apply {
                addView(title(GlobalString.configuration))
                addView(binding.root)
            }
        }
    }

    /**
     * Rclone挂载对象改变按钮点击事件
     */
    fun onMountChangeBtnClick(v: View) {
        runOnScope {
            val context = v.context
            val name = mountName.get()
            val items = mutableListOf<String>()
            for (i in rcloneConfigList.value) items.add(i.name)
            var choice = items.indexOf(name)
            if (choice == -1) {
                choice = 0
            }

            if (items.isNotEmpty()) {
                ListPopupWindow(context).apply {
                    fastInitialize(v, items.toTypedArray(), choice)
                    setOnItemClickListener { _, _, position, _ ->
                        changeMount(rcloneConfigList.value[position])
                        dismiss()
                    }
                    show()
                }
            }
        }
    }

    /**
     * 挂载目录改变按钮点击事件
     */
    fun onMountDestChangeBtnClick(v: View) {
        val name = mountName.get()!!
        if (name != GlobalString.notSelected) {
            materialYouFileExplorer.apply {
                isFile = false
                toExplorer(v.context) { path, _ ->
                    mountDest.set(path)
                    if (rcloneMountMap.value.containsKey(name).not()) {
                        // 挂载哈希表中不存在该条目
                        rcloneMountMap.value[name] = RcloneMount(name)
                    }
                    rcloneMountMap.value[name]?.apply {
                        dest = path
                    }
                    runOnScope {
                        JSON.saveMountHashMapJson(rcloneMountMap.value)
                    }
                }
            }
        }
    }

    /**
     * 切换挂载对象
     */
    private fun changeMount(rcloneConfig: RcloneConfig) {
        runOnScope {
            mountName.set(rcloneConfig.name)

            if (rcloneMountMap.value.containsKey(rcloneConfig.name).not()) {
                // 挂载哈希表中不存在该条目
                rcloneMountMap.value[rcloneConfig.name] = RcloneMount(rcloneConfig.name)
            }
            rcloneMountMap.value[rcloneConfig.name]?.apply {
                if (this.mounted) {
                    mountState.set(GlobalString.mounted)
                    mountIcon.set(App.globalContext.getDrawable(R.drawable.ic_filled_light))
                } else {
                    mountState.set(GlobalString.notMounted)
                    mountIcon.set(App.globalContext.getDrawable(R.drawable.ic_outline_light))
                }

                if (this.dest.isNotEmpty()) {
                    mountDest.set(this.dest)
                } else {
                    mountDest.set(GlobalString.notSelected)
                }
            }
        }
    }

    /**
     * 挂载按钮点击事件
     */
    fun onMountBtnClick(v: View) {
        isChangingMount.set(true)
        val name = mountName.get()
        val dest = mountDest.get()
        if (dest == GlobalString.notSelected) {
            isChangingMount.set(false)
            Toast.makeText(
                App.globalContext,
                GlobalString.mountDirectoryNotSelected,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        rcloneMountMap.value[name]?.apply {
            runOnScope {
                if (this.mounted) {
                    // 当前为已挂载状态 -> 取消挂载
                    if (ExtendCommand.rcloneUnmount(this.dest)) {
                        // 取消挂载成功
                        this.mounted = this.mounted.not()
                        mountState.set(GlobalString.notMounted)
                        mountIcon.set(App.globalContext.getDrawable(R.drawable.ic_outline_light))
                    }
                } else {
                    // 当前为未挂载状态 -> 执行挂载

                    // 确保处于未挂载状态
                    ExtendCommand.rcloneUnmount(this.dest, false)
                    if (ExtendCommand.rcloneMount(this.name, this.dest)) {
                        // 挂载成功
                        this.mounted = this.mounted.not()
                        mountState.set(GlobalString.mounted)
                        mountIcon.set(App.globalContext.getDrawable(R.drawable.ic_filled_light))
                    }
                }
                JSON.saveMountHashMapJson(rcloneMountMap.value)
                isChangingMount.set(false)
            }
        }
    }
}