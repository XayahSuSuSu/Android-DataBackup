package com.xayah.databackup.preference

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import com.xayah.databackup.databinding.PreferenceSwitchBinding

interface SwitchPreference : Preference {
    var icon: Drawable?
    var title: CharSequence?
    var summary: CharSequence?
    var listener: OnChangedListener?
}

fun PreferenceScreen.switch(
    onSwitchEvent: (Boolean) -> Unit = {},
    onCreated: (PreferenceSwitchBinding) -> Unit = {},
    onShowSwitch: Boolean = true,
    @DrawableRes icon: Int? = null,
    @StringRes title: Int? = null,
    @StringRes summary: Int? = null,
    configure: SwitchPreference.() -> Unit = {},
): SwitchPreference {
    val binding = PreferenceSwitchBinding
        .inflate(LayoutInflater.from(context), root, false)

    val impl = object : SwitchPreference {
        override val view: View
            get() = binding.root
        override var icon: Drawable?
            get() = binding.iconView.background
            set(value) {
                binding.iconView.background = value
            }
        override var title: CharSequence?
            get() = binding.titleView.text
            set(value) {
                binding.titleView.text = value
            }
        override var summary: CharSequence?
            get() = binding.summaryView.text
            set(value) {
                binding.summaryView.text = value
            }
        override var listener: OnChangedListener? = null
        override var enabled: Boolean
            get() = binding.root.isEnabled
            set(value) {
                binding.root.isEnabled = value
                binding.root.isFocusable = value
                binding.root.isClickable = value
                binding.root.alpha = if (value) 1.0f else 0.33f
            }

    }

    if (icon != null) {
        impl.icon = ContextCompat.getDrawable(context, icon)
    }

    if (title != null) {
        impl.title = context.getString(title)
    }

    if (summary != null) {
        impl.summary = context.getString(summary)
    }

    impl.configure()

    addElement(impl)

    onCreated(binding)

    binding.switchView.isGone = !onShowSwitch

    binding.root.setOnClickListener {
        binding.switchView.isChecked = !binding.switchView.isChecked
        onSwitchEvent(binding.switchView.isChecked)
        impl.listener?.onChanged()
    }

    binding.switchView.setOnCheckedChangeListener { _, isChecked ->
        onSwitchEvent(isChecked)
        impl.listener?.onChanged()
    }

    return impl
}