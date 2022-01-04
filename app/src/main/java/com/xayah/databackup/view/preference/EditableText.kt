package com.xayah.databackup.view.preference

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R
import com.xayah.databackup.databinding.DialogTextFieldBinding

interface EditableTextPreference : ClickablePreference {
    var placeholder: CharSequence?
    var empty: CharSequence?
    var text: String?
}

fun PreferenceScreen.editableText(
    onCreated: (DialogTextFieldBinding) -> Unit = {},
    onPositiveEvent: (DialogTextFieldBinding) -> Unit = {},
    onNegativeEvent: (DialogTextFieldBinding) -> Unit = {},
    onNeutralEvent: (DialogTextFieldBinding) -> Unit = {},
    @StringRes title: Int,
    summary: CharSequence? = null,
    @DrawableRes icon: Int? = null,
    @StringRes placeholder: Int? = null,
    @StringRes empty: Int? = null,
    configure: EditableTextPreference.() -> Unit = {},
): EditableTextPreference {
    val impl = object : EditableTextPreference, ClickablePreference by clickable(title, icon) {
        override var placeholder: CharSequence? = null
        override var empty: CharSequence? = null
        override var text: String? = null
            set(value) {
                field = value

                when {
                    value == null -> {
                        this.summary = this.placeholder
                    }
                    value.isEmpty() -> {
                        this.summary = this.empty
                    }
                    else -> {
                        this.summary = value
                    }
                }
            }
    }
    var binding: DialogTextFieldBinding

    impl.clicked {
        binding = DialogTextFieldBinding
            .inflate((context as Activity).layoutInflater, root, false)
        binding.textLayout.hint = context.getString(title)
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(binding.root)
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.dialog_positive)) { dialog, _ ->
                onPositiveEvent(
                    binding
                )
                impl.summary = binding.textField.text.toString()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.dialog_negative)) { dialog, _ ->
                onNegativeEvent(
                    binding
                )
                dialog.dismiss()
            }
            .setNeutralButton(context.getString(R.string.dialog_neutral)) { dialog, _ ->
                onNeutralEvent(
                    binding
                )
                impl.summary = binding.textField.text.toString()
                dialog.dismiss()
            }
            .show()
        onCreated(binding)
    }

    if (placeholder != null) {
        impl.placeholder = context.getText(placeholder)
    }

    if (empty != null) {
        impl.empty = context.getText(empty)
    }
    impl.summary = summary
    impl.configure()
    return impl
}