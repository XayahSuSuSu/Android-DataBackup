package com.xayah.databackup.view.card

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.databinding.ViewCardStorageRadioCardBinding
import com.xayah.databackup.util.*
import com.xayah.databackup.view.setLoading
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

    private var internalStoragePath: CharSequence?
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

    private var otgPath: CharSequence?
        get() = binding.materialTextViewOtgPath.text
        set(value) {
            binding.materialTextViewOtgPath.text = value
        }

    private var customProgress: Int
        get() = binding.linearProgressIndicatorCustom.progress
        set(value) {
            binding.linearProgressIndicatorCustom.progress = value
        }

    private var customPath: CharSequence?
        get() = binding.materialTextViewCustomPath.text
        set(value) {
            binding.materialTextViewCustomPath.text = value
        }

    private var radioGroupCheckedIndex: Int
        get() = App.globalContext.readBackupSaveIndex()
        set(value) {
            App.globalContext.saveBackupSaveIndex(value)
            when (value) {
                0 -> {
                    App.globalContext.saveBackupSavePath(internalStoragePath.toString())
                }
                1 -> {
                    App.globalContext.saveBackupSavePath(otgPath.toString())

                }
                2 -> {
                    App.globalContext.saveBackupSavePath(customPath.toString())

                }
            }
            binding.materialRadioButtonInternalStorage.isChecked = (value == 0)
            binding.materialRadioButtonOtg.isChecked = (value == 1)
            binding.materialRadioButtonCustom.isChecked = (value == 2)
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
        binding.materialRadioButtonCustom.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioGroupCheckedIndex = 2
            }
        }
        binding.materialRadioButtonInternalStorage.setOnClickListener {
            initializeGlobalList()
        }
        binding.materialRadioButtonOtg.setOnClickListener {
            initializeGlobalList()

        }
        binding.materialRadioButtonCustom.setOnClickListener {
            initializeGlobalList()
        }
        binding.materialButtonEdit.setOnClickListener {
            val that = this
            materialYouFileExplorer.apply {
                defPath = getPathByIndex()
                isFile = false
                toExplorer(context) { path, _ ->
                    radioGroupCheckedIndex = 2
                    App.globalContext.saveCustomBackupSavePath(path)
                    App.globalContext.saveBackupSavePath(path)
                    update()
                    initializeGlobalList()
                }
            }
        }

        update()
    }

    private fun update() {
        setInternalStorage()
        setOTG()
        setCustom()
        radioGroupCheckedIndex = App.globalContext.readBackupSaveIndex()
    }

    private fun getPathByIndex(): String {
        when (radioGroupCheckedIndex) {
            0 -> {
                return internalStoragePath.toString()
            }
            1 -> {
                return otgPath.toString()
            }
            2 -> {
                return customPath.toString()
            }
        }
        return internalStoragePath.toString()
    }

    private fun setInternalStorage() {
        val path = Path.getExternalStorageDataBackupDirectory()
        // 默认值
        internalStoragePath = GlobalString.fetching
        internalStorageProgress = 0

        val space = Bashrc.getStorageSpace(path)
        val string = if (space.first) space.second else GlobalString.error
        internalStoragePath = path
        if (space.first) {
            try {
                internalStorageProgress = string.split(" ").last().replace("%", "").toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                internalStoragePath = GlobalString.fetchFailed
            }
        } else {
            internalStoragePath = GlobalString.fetchFailed
        }
    }

    private fun setCustom() {
        val path = Path.getCustomDataBackupDirectory()
        // 默认值
        customPath = GlobalString.fetching
        customProgress = 0

        val space = Bashrc.getStorageSpace(path)
        val string = if (space.first) space.second else GlobalString.error
        customPath = path
        if (space.first) {
            try {
                customProgress = string.split(" ").last().replace("%", "").toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                customPath = GlobalString.fetchFailed
            }
        } else {
            customPath = GlobalString.fetchFailed
        }
    }

    private fun setOTG() {
        // 默认值
        otgPath = GlobalString.fetching
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
                        Log.d("fuck", string)
                        otgProgress = string.split(" ").last().replace("%", "").toInt()
                        otgPath = path
                        otgEnabled = true
                    } catch (e: NumberFormatException) {
                        otgPath = GlobalString.fetchFailed
                        e.printStackTrace()
                    }
                }
            } else if (that.first == 1) {
                if (radioGroupCheckedIndex == 1) radioGroupCheckedIndex = 0
                otgPath = GlobalString.unsupportedFormat
            } else {
                if (radioGroupCheckedIndex == 1) radioGroupCheckedIndex = 0
                otgPath = GlobalString.notPluggedIn
            }
        }
    }

    private fun initializeGlobalList() {
        BottomSheetDialog(context).apply {
            setLoading()
            CoroutineScope(Dispatchers.IO).launch {
                App.initializeGlobalList()
                dismiss()
            }
        }
    }
}