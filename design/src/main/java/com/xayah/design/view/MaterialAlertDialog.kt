package com.xayah.design.view

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.design.R


fun MaterialAlertDialogBuilder.setWithNormalMessage(
    title: String,
    message: String,
    cancelable: Boolean = true,
    callback: () -> Unit = {}
) {
    this.apply {
        setTitle(title)
        setCancelable(cancelable)
        setMessage(message)
        setPositiveButton(this.context.getString(R.string.dialog_positive)) { _, _ -> callback() }
        show()
    }
}

fun MaterialAlertDialogBuilder.setWithConfirm(
    message: String,
    callback: () -> Unit
) {
    this.apply {
        setTitle(context.getString(R.string.tips))
        setCancelable(true)
        setMessage(message)
        setNegativeButton(context.getString(R.string.dialog_negative)) { _, _ -> }
        setPositiveButton(context.getString(R.string.dialog_positive)) { _, _ -> callback() }
        show()
    }
}
