package com.xayah.databackup.view.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.databinding.BaseObservable
import androidx.databinding.ObservableField
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.RcloneMount
import com.xayah.databackup.databinding.ViewCardStorageRadioCardBinding
import com.xayah.databackup.databinding.ViewCardStorageRadioCardItemBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.util.dp
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class StorageItem : BaseObservable() {
    var enable: ObservableField<Boolean> = ObservableField(true)
    var name: ObservableField<String> = ObservableField("")
    var progress: ObservableField<Int> = ObservableField(0)
    var path: ObservableField<String> = ObservableField("")
    var display: ObservableField<String> = ObservableField("")
}


class StorageRadioCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewOutlinedStyle
) : MaterialCardView(context, attributeSet, defStyleAttr) {
    private val binding =
        ViewCardStorageRadioCardBinding.inflate(LayoutInflater.from(context), this, true)

    // 挂载哈希表
    private val rcloneMountMap by lazy {
        MutableStateFlow(hashMapOf<String, RcloneMount>())
    }

    /**
     * 全局单例对象
     */
    val globalObject = GlobalObject.getInstance()

    // 绑定列表
    private val itemBindings = mutableListOf<ViewCardStorageRadioCardItemBinding>()

    var title: CharSequence?
        get() = binding.materialTextViewTitle.text
        set(value) {
            binding.materialTextViewTitle.text = value
        }

    var text: CharSequence?
        get() = binding.materialTextViewText.text
        set(value) {
            binding.materialTextViewText.text = value
        }

    private var radioGroupCheckedIndex: Int
        get() = App.globalContext.readBackupSaveIndex()
        set(value) {
            App.globalContext.saveBackupSaveIndex(value)
            for ((index, i) in itemBindings.withIndex()) {
                if (value == index) {
                    i.item?.apply {
                        App.globalContext.saveBackupSavePath(this.path.get())
                        SafeFile.mkdirs(Path.getShellLogPath())
                        SafeFile.mkdirs(Path.getActionLogPath())
                        Logcat.refreshInstance()
                        globalObject.appInfoBackupMap.value.clear()
                        globalObject.appInfoRestoreMap.value.clear()
                        globalObject.mediaInfoBackupMap.value.clear()
                        globalObject.mediaInfoRestoreMap.value.clear()
                    }
                }
                i.materialRadioButton.isChecked = value == index
            }
        }

    private lateinit var materialYouFileExplorer: MaterialYouFileExplorer

    fun setMaterialYouFileExplorer(mMaterialYouFileExplorer: MaterialYouFileExplorer) {
        materialYouFileExplorer = mMaterialYouFileExplorer
    }

    init {
        context.theme.obtainStyledAttributes(
            attributeSet, R.styleable.StorageRadioCard, defStyleAttr, 0
        ).apply {
            try {
                title = getString(R.styleable.StorageRadioCard_title)
                text = getString(R.styleable.StorageRadioCard_text)
            } finally {
                recycle()
            }
        }

        radius = 24.dp.toFloat()
        setContentPadding(16.dp, 8.dp, 16.dp, 16.dp)
        binding.materialButtonEdit.setOnClickListener {
            pickPath()
        }

        update()
    }

    private fun pickPath() {
        materialYouFileExplorer.apply {
            isFile = false
            toExplorer(context) { actualPath, _ ->
                radioGroupCheckedIndex = 0
                itemBindings.first().item?.apply {
                    path.set(actualPath)
                    display.set(actualPath)
                }
                App.globalContext.saveBackupSavePath(actualPath)
                App.globalContext.saveCustomBackupSavePath(actualPath)
                update()
            }
        }
    }

    private fun update() {
        CoroutineScope(Dispatchers.Main).launch {
            rcloneMountMap.emit(ExtendCommand.getRcloneMountMap())
            binding.linearLayoutRoot.removeAllViews()
            itemBindings.clear()
            setInternalStorage()
            setExternalStorage()
            setMount()
            radioGroupCheckedIndex = App.globalContext.readBackupSaveIndex()
            if (radioGroupCheckedIndex >= itemBindings.size) radioGroupCheckedIndex = 0
        }
    }

    private suspend fun setMount() {
        rcloneMountMap.value.forEach { (_, rcloneMount) ->
            if (rcloneMount.mounted) {
                val mountBinding = ViewCardStorageRadioCardItemBinding.inflate(
                    LayoutInflater.from(context),
                    binding.linearLayoutRoot,
                    true
                ).apply {
                    item = StorageItem().apply {
                        name.set(rcloneMount.name)
                        progress.set(0)
                        path.set(rcloneMount.dest)
                        materialRadioButton.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                radioGroupCheckedIndex = itemBindings.size - 1
                            }
                        }
                    }
                }
                itemBindings.add(mountBinding)

                mountBinding.item?.apply {
                    // 默认值
                    display.set(GlobalString.fetching)
                    progress.set(0)

                    val space = Bashrc.getStorageSpace(path.get()!!)
                    val string = if (space.first) space.second else GlobalString.error
                    display.set(path.get()!!)
                    if (space.first) {
                        try {
                            progress.set(string.split(" ").last().replace("%", "").toInt())
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            radioGroupCheckedIndex = 0
                            enable.set(false)
                            display.set(GlobalString.fetchFailed)
                        }
                    } else {
                        radioGroupCheckedIndex = 0
                        enable.set(false)
                        display.set(GlobalString.fetchFailed)
                    }
                }
            }
        }
    }

    private suspend fun setInternalStorage() {
        val internalStorageBinding = ViewCardStorageRadioCardItemBinding.inflate(
            LayoutInflater.from(context),
            binding.linearLayoutRoot,
            true
        ).apply {
            item = StorageItem().apply {
                name.set(GlobalString.internalStorage)
                progress.set(0)
                path.set(App.globalContext.readCustomBackupSavePath())
                materialRadioButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        radioGroupCheckedIndex = 0
                    }
                }
            }
        }
        itemBindings.add(internalStorageBinding)

        internalStorageBinding.item?.apply {
            Command.ls(path.get()!!).apply {
                if (!this) {
                    MaterialAlertDialogBuilder(context).apply {
                        setTitle(GlobalString.tips)
                        setCancelable(false)
                        setMessage(GlobalString.backupDirNotExist)
                        setNeutralButton(GlobalString.pickDir) { _, _ -> pickPath() }
                        setPositiveButton(GlobalString.create) { _, _ ->
                            CoroutineScope(Dispatchers.Main).launch {
                                Command.mkdir(path.get()!!)
                                update()
                            }
                        }
                        show()
                    }
                }
            }
            // 默认值
            display.set(GlobalString.fetching)
            progress.set(0)

            val space = Bashrc.getStorageSpace(path.get()!!)
            val string = if (space.first) space.second else GlobalString.error
            display.set(path.get()!!)
            if (space.first) {
                try {
                    progress.set(string.split(" ").last().replace("%", "").toInt())
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    display.set(GlobalString.fetchFailed)
                }
            } else {
                display.set(GlobalString.fetchFailed)
            }
        }
    }

    /**
     * 设置外置存储设备
     */
    private suspend fun setExternalStorage() {
        // 列出外置存储设备: /mnt/media_rw/E7F9-FA61 exfat
        Bashrc.listExternalStorage().apply {
            if (this.first) {
                for (i in this.second) {
                    try {
                        val (path, type) = i.split(" ")
                        val otgBinding = ViewCardStorageRadioCardItemBinding.inflate(
                            LayoutInflater.from(context),
                            binding.linearLayoutRoot,
                            true
                        ).apply {
                            item = StorageItem().apply {
                                this.name.set(path.split("/").last())
                                this.progress.set(0)
                                this.display.set(i)

                                // 创建备份目录
                                val outPath = "${path}/DataBackup"
                                this.path.set(outPath)

                                // 检测空间占用
                                val space = Bashrc.getStorageSpace(outPath)
                                if (space.first) {
                                    try {
                                        this.progress.set(
                                            space.second.split(" ").last().replace("%", "").toInt()
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                // 检测格式是否支持
                                val supportedFormat =
                                    mutableListOf(
                                        "sdfat",
                                        "fuseblk",
                                        "exfat",
                                        "ntfs",
                                        "ext4",
                                        "f2fs"
                                    )
                                val support = type.lowercase() in supportedFormat
                                this.enable.set(support)
                                if (support.not()) {
                                    this.name.set(GlobalString.unsupportedFormat)
                                }

                                materialRadioButton.setOnCheckedChangeListener { _, isChecked ->
                                    if (isChecked) {
                                        radioGroupCheckedIndex = 1
                                    }
                                }
                            }
                        }
                        itemBindings.add(otgBinding)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}