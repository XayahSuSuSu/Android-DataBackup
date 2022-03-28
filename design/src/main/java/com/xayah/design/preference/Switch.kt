package com.xayah.design.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.xayah.design.R
import com.xayah.design.databinding.PreferenceSwitchBinding

class Switch @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes) {
    private val binding = PreferenceSwitchBinding
        .inflate(LayoutInflater.from(context), this, true)

    var icon: Drawable?
        get() = binding.iconView.background
        set(value) {
            binding.iconView.background = value
        }

    var title: CharSequence?
        get() = binding.titleView.text
        set(value) {
            binding.titleView.text = value
        }

    var summary: CharSequence?
        get() = binding.summaryView.text
        set(value) {
            binding.summaryView.text = value
            binding.summaryView.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var checked: Boolean
        get() = binding.switchView.isChecked
        set(value) {
            binding.switchView.isChecked = value
        }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.root.setOnClickListener(l)
    }

    fun setOnCheckedChangeListener(listener: ((buttonView: CompoundButton, isChecked: Boolean) -> Unit)) {
        binding.switchView.setOnCheckedChangeListener(listener)
    }

    init {
        binding.root.setOnClickListener {
            binding.switchView.performClick()
        }

        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.Switch,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                icon = getDrawable(R.styleable.Switch_icon)
                title = getString(R.styleable.Switch_title)
                summary = getString(R.styleable.Switch_summary)
                checked = getBoolean(R.styleable.Switch_checked, false)
            } finally {
                recycle()
            }
        }
    }
}