package com.xayah.design.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.widget.ListPopupWindow
import com.xayah.design.R
import com.xayah.design.adapter.PopupListAdapter
import com.xayah.design.databinding.PreferenceClickableBinding
import com.xayah.design.util.getPixels
import com.xayah.design.util.measureWidth

class SelectableList @JvmOverloads constructor(
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

    override fun setOnClickListener(l: OnClickListener?) {
        binding.root.setOnClickListener(l)
    }

    private lateinit var onConfirmListener: (v: SelectableList, choice: Int) -> Unit

    fun setOnConfirmListener(listener: ((v: SelectableList, choice: Int) -> Unit)) {
        onConfirmListener = listener
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

    private var items: Array<String> = arrayOf()

    fun setItems(items: Array<String>) {
        this.items = items
        refreshSummary()
    }

    private fun refreshSummary() {
        if (choice <= items.size - 1)
            summary = items[choice]
    }

    var choice = 0
        set(value) {
            field = value
            refreshSummary()
        }

    init {
        val that = this
        binding.root.setOnClickListener {
            ListPopupWindow(context).apply {
                val adapter = PopupListAdapter(
                    context,
                    items.toList(),
                    choice,
                )
                setAdapter(adapter)
                anchorView = binding.root
                width = adapter.measureWidth(context)
                    .coerceAtLeast(context.getPixels(R.dimen.dialog_menu_min_width))
                isModal = true
                horizontalOffset = context.getPixels(R.dimen.item_header_component_size) +
                        context.getPixels(R.dimen.item_header_margin) * 2
                setOnItemClickListener { _, _, position, _ ->
                    dismiss()
                    choice = position
                    refreshSummary()
                    if (::onConfirmListener.isInitialized)
                        onConfirmListener.invoke(that, choice)
                }
                show()
            }
        }
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.SelectableList,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                icon = getDrawable(R.styleable.SelectableList_icon)
                title = getString(R.styleable.SelectableList_title)
                summary = getString(R.styleable.SelectableList_summary)
                choice = getInt(R.styleable.SelectableList_choice, 0)
            } finally {
                recycle()
            }
        }
    }
}