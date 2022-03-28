package com.xayah.design

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import com.google.android.material.card.MaterialCardView
import com.xayah.design.databinding.ComponentLargeActionLabelBinding
import com.xayah.design.util.resolveClickableAttrs
import com.xayah.design.util.selectableItemBackground

class LargeActionCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : MaterialCardView(context, attributeSet, defStyleAttr) {
    private val binding = ComponentLargeActionLabelBinding
        .inflate(LayoutInflater.from(context), this, true)

    var text: CharSequence?
        get() = binding.textView.text
        set(value) {
            binding.textView.text = value
        }

    var subtext: CharSequence?
        get() = binding.subtextView.text
        set(value) {
            binding.subtextView.text = value
        }

    var icon: Drawable?
        get() = binding.iconView.background
        set(value) {
            binding.iconView.background = value
        }

    init {
        context.resolveClickableAttrs(attributeSet, defStyleAttr) {
            isFocusable = focusable(true)
            isClickable = clickable(true)
            foreground = foreground() ?: context.selectableItemBackground
        }

        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.LargeActionCard,
            defStyleAttr,
            0
        ).apply {
            try {
                icon = getDrawable(R.styleable.LargeActionCard_icon)
                text = getString(R.styleable.LargeActionCard_text)
                subtext = getString(R.styleable.LargeActionCard_subtext)
            } finally {
                recycle()
            }
        }

        minimumHeight = getPixels(R.dimen.large_action_card_min_height)
        radius = getPixels(R.dimen.large_action_card_radius).toFloat()
//        elevation = getPixels(R.dimen.large_action_card_elevation).toFloat()
    }

    private fun getPixels(@DimenRes resId: Int): Int {
        return resources.getDimensionPixelSize(resId)
    }

}