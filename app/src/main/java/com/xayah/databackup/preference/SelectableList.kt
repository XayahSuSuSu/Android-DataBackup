package com.xayah.databackup.preference

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R

interface SelectableListPreference : ClickablePreference {
    var text: String?
}

fun PreferenceScreen.selectableList(
    onPositiveEvent: (String) -> Unit = {},
    @StringRes title: Int,
    items: Array<String>,
    defaultItem: (Int) -> Int,
    summary: CharSequence? = null,
    @DrawableRes icon: Int? = null,
    configure: SelectableListPreference.() -> Unit = {},
): SelectableListPreference {
    val impl = object : SelectableListPreference, ClickablePreference by clickable(title, icon) {
        override var text: String? = null
            set(value) {
                field = value
                this.summary = value
            }
    }
    var chooseIndex = 0

    impl.clicked {
        val index = defaultItem(0)
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setCancelable(true)
            .setSingleChoiceItems(
                items,
                index
            ) { _, which ->
                chooseIndex = which
            }
            .setPositiveButton(context.getString(R.string.dialog_positive)) { dialog, _ ->
                onPositiveEvent(items[chooseIndex])
                impl.summary = items[chooseIndex]
                dialog.dismiss()
            }
            .show()
    }
    impl.summary = summary
    impl.configure()
    return impl
}