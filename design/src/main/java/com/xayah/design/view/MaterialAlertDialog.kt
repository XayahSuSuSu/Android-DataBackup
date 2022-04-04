package com.xayah.design.view

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.design.R


fun MaterialAlertDialogBuilder.setWithNormalMessage(title: String, message: String) {
    this.apply {
        setTitle(title)
        setCancelable(true)
        setMessage(message)
        setPositiveButton(this.context.getString(R.string.dialog_positive)) { _, _ -> }
        show()
    }
}
