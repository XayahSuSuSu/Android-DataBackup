package com.xayah.design.preference

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.design.R
import com.xayah.design.databinding.DesignDialogTextFieldBinding
import com.xayah.design.databinding.PreferenceClickableBinding

class EditableText @JvmOverloads constructor(
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

    var defaultValue: CharSequence? = ""

    override fun setOnClickListener(l: OnClickListener?) {
        binding.root.setOnClickListener(l)
    }

    private lateinit var onConfirmListener: (v: EditableText, content: CharSequence?) -> Unit

    fun setOnConfirmListener(listener: ((v: EditableText, content: CharSequence?) -> Unit)) {
        onConfirmListener = listener
    }

    init {
        binding.root.setOnClickListener {
            val bindingDialogTextField = DesignDialogTextFieldBinding.inflate(
                    (context as Activity).layoutInflater,
                    binding.root as ViewGroup,
                    false
                )
            bindingDialogTextField.textLayout.hint = title
            bindingDialogTextField.textField.setText(summary)
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(bindingDialogTextField.root)
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.dialog_positive)) { _, _ ->
                    summary = bindingDialogTextField.textField.text
                    if (::onConfirmListener.isInitialized)
                        onConfirmListener.invoke(this, summary)
                }
                .setNegativeButton(context.getString(R.string.dialog_negative)) { _, _ -> }
                .setNeutralButton(context.getString(R.string.dialog_neutral)) { _, _ ->
                    summary = defaultValue
                    onConfirmListener.invoke(this, summary)
                }
                .show()
        }
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.EditableText,
            defStyleAttr,
            defStyleRes
        ).apply {
            try {
                icon = getDrawable(R.styleable.EditableText_icon)
                title = getString(R.styleable.EditableText_title)
                summary = getString(R.styleable.EditableText_summary)
                defaultValue = getString(R.styleable.EditableText_defaultValue)
            } finally {
                recycle()
            }
        }
    }
}