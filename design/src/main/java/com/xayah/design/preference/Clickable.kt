package com.xayah.design.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.xayah.design.R
import com.xayah.design.databinding.PreferenceClickableBinding

class Clickable @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attributeSet, defStyleAttr, defStyleRes) {
    private val binding = PreferenceClickableBinding
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

    fun setRound(isRound: Boolean) {
        if (isRound)
            binding.iconView.roundPercent = 1F
        else
            binding.iconView.roundPercent = 0F
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.root.setOnClickListener(l)
    }

    fun setIconButtonVisibility(isVisible: Boolean) {
        if (isVisible)
            binding.iconButton.visibility = View.VISIBLE
        else
            binding.iconButton.visibility = View.GONE
    }

    fun setOnIconButtonClickListener(listener: ((buttonView: View) -> Unit)) {
        binding.iconButton.setOnClickListener(listener)
    }

    init {
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.ClickableView,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                icon = getDrawable(R.styleable.ClickableView_icon)
                title = getString(R.styleable.ClickableView_title)
                summary = getString(R.styleable.ClickableView_summary)
            } finally {
                recycle()
            }
        }
    }
}