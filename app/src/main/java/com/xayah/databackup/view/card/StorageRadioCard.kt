package com.xayah.databackup.view.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import com.google.android.material.card.MaterialCardView
import com.xayah.databackup.R
import com.xayah.databackup.databinding.ViewCardStorageRadioCardBinding
import com.xayah.databackup.util.Bashrc
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Path
import com.xayah.databackup.view.util.dp

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

    private var internalStorageEnabled: Boolean
        get() = binding.materialRadioButtonInternalStorage.isEnabled
        set(value) {
            binding.materialRadioButtonInternalStorage.isEnabled = value
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

    var index = -1

    var radioGroupCheckedIndex: Int
        get() = index
        set(value) {
            index = value
            binding.materialRadioButtonInternalStorage.isChecked = (index == 0)
            binding.materialRadioButtonOtg.isChecked = (index == 1)
        }

    private var onCheckedChangeListener: (v: StorageRadioCard, path: String) -> Unit = { _, _ -> }

    fun setOnCheckedChangeListener(listener: ((v: StorageRadioCard, path: String) -> Unit)) {
        onCheckedChangeListener = listener
    }

    init {
        context.theme.obtainStyledAttributes(
            attributeSet, R.styleable.StorageRadioCard, defStyleAttr, 0
        ).apply {
            try {
                title = getString(R.styleable.StorageRadioCard_title)
            } finally {
                recycle()
            }
        }

        radius = 24.dp.toFloat()
        setContentPadding(16.dp, 16.dp, 16.dp, 16.dp)

        binding.materialRadioButtonInternalStorage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioGroupCheckedIndex = 0
                update()
                onCheckedChangeListener.invoke(this, internalStoragePath.toString())
            }
        }
        binding.materialRadioButtonOtg.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioGroupCheckedIndex = 1
                update()
                onCheckedChangeListener.invoke(this, otgPath.toString())
            }
        }

        radioGroupCheckedIndex = 0
        update()
    }

    private fun update() {
        setInternalStorage()
        setOTG()
    }

    fun getPathByIndex(index: Int): String {
        return if (index == 0) {
            internalStoragePath.toString()
        } else {
            otgPath.toString()
        }
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

    private fun setOTG() {
        // 默认值
        otgPath = GlobalString.fetching
        otgProgress = 0
        otgEnabled = false
        // 检查OTG连接情况
        Bashrc.checkOTG().apply {
            val that = this
            if (that.first == 0) {
                val space = Bashrc.getStorageSpace(that.second)
                if (space.first) {
                    try {
                        val string = space.second
                        otgProgress = string.split(" ").last().replace("%", "").toInt()
                        otgPath = that.second + "/DataBackup"
                        otgEnabled = true
                    } catch (e: NumberFormatException) {
                        otgPath = GlobalString.fetchFailed
                        e.printStackTrace()
                    }
                }
            } else if (that.first == 1) {
                radioGroupCheckedIndex = 0
                otgPath = GlobalString.unsupportedFormat
            } else {
                radioGroupCheckedIndex = 0
                otgPath = GlobalString.notPluggedIn
            }
        }
    }
}