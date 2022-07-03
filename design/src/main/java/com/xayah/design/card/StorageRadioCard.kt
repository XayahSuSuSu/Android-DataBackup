package com.xayah.design.card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.compose.runtime.mutableStateListOf
import com.google.android.material.card.MaterialCardView
import com.xayah.design.R
import com.xayah.design.databinding.ComponentStorageRadioCardBinding
import com.xayah.design.util.dp

class StorageRadioCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewOutlinedStyle
) : MaterialCardView(context, attributeSet, defStyleAttr) {
    private val binding =
        ComponentStorageRadioCardBinding.inflate(LayoutInflater.from(context), this, true)

    var title: CharSequence?
        get() = binding.materialTextViewTitle.text
        set(value) {
            binding.materialTextViewTitle.text = value
        }

    var internalStorageEnabled: Boolean
        get() = binding.materialRadioButtonInternalStorage.isEnabled
        set(value) {
            binding.materialRadioButtonInternalStorage.isEnabled = value
        }

    var internalStorageProgress: Int
        get() = binding.linearProgressIndicatorInternalStorage.progress
        set(value) {
            binding.linearProgressIndicatorInternalStorage.progress = value
        }

    var internalStoragePath: CharSequence?
        get() = binding.materialTextViewInternalStoragePath.text
        set(value) {
            binding.materialTextViewInternalStoragePath.text = value
        }

    var otgEnabled: Boolean
        get() = binding.materialRadioButtonOtg.isEnabled
        set(value) {
            binding.materialRadioButtonOtg.isEnabled = value
        }

    var otgProgress: Int
        get() = binding.linearProgressIndicatorOtg.progress
        set(value) {
            binding.linearProgressIndicatorOtg.progress = value
        }

    var otgPath: CharSequence?
        get() = binding.materialTextViewOtgPath.text
        set(value) {
            binding.materialTextViewOtgPath.text = value
        }

    private var radioGroupCheckedId: Int
        get() = 0
        set(value) {
            updateRadioButton(value)
        }

    var radioGroupCheckedIndex: Int
        get() = mutableStateListOf(
            R.id.materialRadioButton_internal_storage,
            R.id.materialRadioButton_otg
        ).indexOf(radioGroupCheckedId)
        set(value) {
            radioGroupCheckedId = mutableStateListOf(
                R.id.materialRadioButton_internal_storage,
                R.id.materialRadioButton_otg
            )[value]
        }

    private var onConfirmListener: (v: StorageRadioCard, index: Int) -> Unit = { _, _ -> }

    fun setOnCheckedChangeListener(listener: ((v: StorageRadioCard, index: Int) -> Unit)) {
        onConfirmListener = listener
    }

    init {
        context.theme.obtainStyledAttributes(
            attributeSet, R.styleable.StorageRadioCard, defStyleAttr, 0
        ).apply {
            try {
                title = getString(R.styleable.StorageRadioCard_title)
                radioGroupCheckedIndex =
                    getInt(R.styleable.StorageRadioCard_radioGroupCheckedIndex, 0)
                internalStorageEnabled =
                    getBoolean(R.styleable.StorageRadioCard_internalStorageEnabled, true)
                internalStorageProgress =
                    getInt(R.styleable.StorageRadioCard_internalStorageProgress, 0)
                internalStoragePath = getString(R.styleable.StorageRadioCard_internalStoragePath)
                internalStorageEnabled = getBoolean(R.styleable.StorageRadioCard_otgEnabled, true)
                otgProgress = getInt(R.styleable.StorageRadioCard_otgProgress, 0)
                otgPath = getString(R.styleable.StorageRadioCard_otgPath)
            } finally {
                recycle()
            }
        }

        radius = 24.dp.toFloat()
        setContentPadding(16.dp, 16.dp, 16.dp, 16.dp)

        radioGroupCheckedIndex = 0

        val that = this

        binding.materialRadioButtonInternalStorage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) radioGroupCheckedIndex = 0
            onConfirmListener.invoke(that, 0)
        }
        binding.materialRadioButtonOtg.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) radioGroupCheckedIndex = 1
            onConfirmListener.invoke(that, 1)
        }
    }

    private fun updateRadioButton(id: Int) {
        binding.materialRadioButtonInternalStorage.isChecked =
            (id == R.id.materialRadioButton_internal_storage)
        binding.materialRadioButtonOtg.isChecked = (id == R.id.materialRadioButton_otg)
    }
}