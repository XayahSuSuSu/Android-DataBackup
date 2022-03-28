package com.xayah.design.preference

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.xayah.design.R
import com.xayah.design.databinding.PreferenceCategoryBinding

class Category @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes) {
    private val binding = PreferenceCategoryBinding
        .inflate(LayoutInflater.from(context), this, true)

    var text: CharSequence?
        get() = binding.textView.text
        set(value) {
            binding.textView.text = value
        }

    init {
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.Category,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                text = getString(R.styleable.Category_text)
            } finally {
                recycle()
            }
        }
    }
}