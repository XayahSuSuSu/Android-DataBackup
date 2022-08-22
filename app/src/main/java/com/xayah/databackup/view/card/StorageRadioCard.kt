package com.xayah.databackup.view.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.databinding.ViewCardStorageRadioCardBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.util.dp
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StorageRadioCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewOutlinedStyle
) : MaterialCardView(context, attributeSet, defStyleAttr) {
    private val binding =
        ViewCardStorageRadioCardBinding.inflate(LayoutInflater.from(context), this, true)

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

    private var internalStorageProgress: Int
        get() = binding.linearProgressIndicatorInternalStorage.progress
        set(value) {
            binding.linearProgressIndicatorInternalStorage.progress = value
        }

    private var internalStoragePath = App.globalContext.readCustomBackupSavePath()
    private var internalStoragePathDisplay: CharSequence?
        get() = binding.materialTextViewInternalStoragePath.text
        set(value) {
            binding.materialTextViewInternalStoragePath.text = value
        }

    private var otgEnabled: Boolean
        get() = binding.materialRadioButtonOtg.isEnabled
        set(value) {
            binding.materialRadioButtonOtg.isEnabled = value
        }

    private var otgProgress: Int
        get() = binding.linearProgressIndicatorOtg.progress
        set(value) {
            binding.linearProgressIndicatorOtg.progress = value
        }

    private var otgPath = ""
    private var otgPathDisplay: CharSequence?
        get() = binding.materialTextViewOtgPath.text
        set(value) {
            binding.materialTextViewOtgPath.text = value
        }

    private var radioGroupCheckedIndex: Int
        get() = App.globalContext.readBackupSaveIndex()
        set(value) {
            App.globalContext.saveBackupSaveIndex(value)
            when (value) {
                0 -> {
                    App.globalContext.saveBackupSavePath(internalStoragePath)
                }
                1 -> {
                    App.globalContext.saveBackupSavePath(otgPath)

                }
            }
            binding.materialRadioButtonInternalStorage.isChecked = (value == 0)
            binding.materialRadioButtonOtg.isChecked = (value == 1)
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

        binding.materialRadioButtonInternalStorage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioGroupCheckedIndex = 0
            }
        }
        binding.materialRadioButtonOtg.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioGroupCheckedIndex = 1
            }
        }
        binding.materialButtonEdit.setOnClickListener {
            pickPath()
        }

        update()
    }

    private fun pickPath() {
        materialYouFileExplorer.apply {
            isFile = false
            toExplorer(context) { path, _ ->
                radioGroupCheckedIndex = 0
                internalStoragePath = path
                internalStoragePathDisplay = path
                App.globalContext.saveBackupSavePath(path)
                App.globalContext.saveCustomBackupSavePath(path)
                update()
            }
        }
    }

    private fun update() {
        CoroutineScope(Dispatchers.Main).launch {
            setInternalStorage()
            setOTG()
            radioGroupCheckedIndex = App.globalContext.readBackupSaveIndex()
        }
    }

    private suspend fun setInternalStorage() {
        val path = internalStoragePath
        Command.ls(path).apply {
            if (!this) {
                MaterialAlertDialogBuilder(context).apply {
                    setTitle(GlobalString.tips)
                    setCancelable(false)
                    setMessage(GlobalString.backupDirNotExist)
                    setNeutralButton(GlobalString.pickDir) { _, _ -> pickPath() }
                    setPositiveButton(GlobalString.create) { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            Command.mkdir(path)
                            update()
                        }
                    }
                    show()
                }
            }
        }
        // 默认值
        internalStoragePathDisplay = GlobalString.fetching
        internalStorageProgress = 0

        val space = Bashrc.getStorageSpace(path)
        val string = if (space.first) space.second else GlobalString.error
        internalStoragePathDisplay = path
        if (space.first) {
            try {
                internalStorageProgress = string.split(" ").last().replace("%", "").toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                internalStoragePathDisplay = GlobalString.fetchFailed
            }
        } else {
            internalStoragePathDisplay = GlobalString.fetchFailed
        }
    }

    private suspend fun setOTG() {
        // 默认值
        otgPathDisplay = GlobalString.fetching
        otgProgress = 0
        otgEnabled = false
        // 检查OTG连接情况
        Bashrc.checkOTG().apply {
            val that = this
            if (that.first == 0) {
                val path = "${that.second}/DataBackup"
                Command.mkdir(path)
                val space = Bashrc.getStorageSpace(path)
                if (space.first) {
                    try {
                        val string = space.second
                        otgProgress = string.split(" ").last().replace("%", "").toInt()
                        otgPathDisplay = path
                        otgPath = path
                        otgEnabled = true
                    } catch (e: NumberFormatException) {
                        otgPathDisplay = GlobalString.fetchFailed
                        e.printStackTrace()
                    }
                }
            } else if (that.first == 1) {
                if (radioGroupCheckedIndex == 1) radioGroupCheckedIndex = 0
                otgPathDisplay = GlobalString.unsupportedFormat
            } else {
                if (radioGroupCheckedIndex == 1) radioGroupCheckedIndex = 0
                otgPathDisplay = GlobalString.notPluggedIn
            }
        }
    }
}