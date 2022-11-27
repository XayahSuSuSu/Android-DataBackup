package com.xayah.databackup.view.card

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.AttrRes
import androidx.core.view.children
import com.google.android.material.card.MaterialCardView
import com.xayah.databackup.R
import com.xayah.databackup.databinding.ViewCardLazyCardBinding

class LazyCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewOutlinedStyle
) : MaterialCardView(context, attributeSet, defStyleAttr) {
    private val binding =
        ViewCardLazyCardBinding.inflate(LayoutInflater.from(context), this, true)

    private var lazy: Boolean = false

    init {
        context.theme.obtainStyledAttributes(
            attributeSet, R.styleable.LazyCard, defStyleAttr, 0
        ).apply {
            try {
                lazy = getBoolean(R.styleable.LazyCard_lazy, false)
            } finally {
                recycle()
            }
        }

    }

    fun setInitialized() {
        for (i in children) {
            i.visibility = View.VISIBLE
        }
        getChildAt(0).visibility = View.GONE
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (lazy) {
            for (i in children) {
                i.visibility = View.GONE
            }
            getChildAt(0).visibility = View.VISIBLE
        } else {
            getChildAt(0).visibility = View.GONE
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }
}
