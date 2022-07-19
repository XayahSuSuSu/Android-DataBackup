package com.xayah.databackup.view.util

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.R

fun MaterialAlertDialogBuilder.setWithConfirm(
    message: String,
    callback: () -> Unit
) {
    this.apply {
        setTitle(context.getString(R.string.tips))
        setCancelable(true)
        setMessage(message)
        setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
        setPositiveButton(context.getString(R.string.confirm)) { _, _ -> callback() }
        show()
    }
}

fun MaterialAlertDialogBuilder.setWithRestartApp(activity: AppCompatActivity) {
    this.apply {
        setTitle(context.getString(R.string.tips))
        setCancelable(false)
        setMessage("请重启应用")
        setPositiveButton(context.getString(R.string.confirm)) { _, _ -> activity.finish() }
        show()
    }
}