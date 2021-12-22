package com.xayah.databackup.preference

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.xayah.databackup.databinding.PreferenceCategoryBinding

fun PreferenceScreen.category(
    @StringRes text: Int,
) {
    val binding = PreferenceCategoryBinding
        .inflate(LayoutInflater.from(context), root, false)

    binding.textView.text = context.getString(text)

    addElement(object : Preference {
        override val view: View
            get() = binding.root
        override var enabled: Boolean
            get() = binding.root.isEnabled
            set(value) {
                binding.root.isEnabled = value
            }
    })
}