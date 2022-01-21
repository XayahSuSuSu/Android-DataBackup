package com.xayah.databackup.view.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.databinding.PreferenceClickableBinding

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

    private var summary: CharSequence?
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

    private var items: Array<String> = arrayOf()

    fun setItems(items: Array<String>) {
        this.items = items
        refreshSummary()
    }

    private fun refreshSummary() {
        if (choice <= items.size - 1)
            summary = items[choice]
    }

    private var choice = 0
        set(value) {
            field = value
            refreshSummary()
        }

    init {
        binding.root.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setCancelable(true)
                .setSingleChoiceItems(
                    items,
                    choice
                ) { _, which ->
                    choice = which
                }
                .setPositiveButton(context.getString(R.string.dialog_positive)) { _, _ ->
                    refreshSummary()
                    if (::onConfirmListener.isInitialized)
                        onConfirmListener.invoke(this, choice)
                }
                .show()
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