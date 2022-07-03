package com.xayah.databackup.fragment.backup

import android.content.Intent
import android.view.View
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.activity.backup.list.app.BackupAppListActivity
import com.xayah.databackup.activity.backup.processing.app.BackupProcessingActivity
import com.xayah.databackup.util.*
import com.xayah.design.adapter.PopupListAdapter
import com.xayah.design.util.getPixels
import com.xayah.design.util.measureWidth

class BackupViewModel : ViewModel() {
    var backupUser = ObservableField(App.globalContext.readBackupUser())
    var restoreUser = ObservableField(App.globalContext.readRestoreUser())
    var appNum = ObservableField("0")
    var dataNum = ObservableField("0")

    fun onChangeUser(v: View) {
        val context = v.context
        val items =
            if (Bashrc.listUsers().first) Bashrc.listUsers().second.toTypedArray() else arrayOf("0")
        var choice = 0
        when (v.id) {
            R.id.materialButton_change_backup_user -> {
                choice = items.indexOf(App.globalContext.readBackupUser())
            }
            R.id.materialButton_change_restore_user -> {
                choice = items.indexOf(App.globalContext.readRestoreUser())
            }
        }
        ListPopupWindow(context).apply {
            val adapter = PopupListAdapter(
                context,
                items.toList(),
                choice,
            )
            setAdapter(adapter)
            anchorView = v
            width = adapter.measureWidth(context)
                .coerceAtLeast(context.getPixels(com.xayah.design.R.dimen.dialog_menu_min_width))
            isModal = true
            horizontalOffset =
                context.getPixels(com.xayah.design.R.dimen.item_header_component_size) + context.getPixels(
                    com.xayah.design.R.dimen.item_header_margin
                ) * 2
            setOnItemClickListener { _, _, position, _ ->
                when (v.id) {
                    R.id.materialButton_change_backup_user -> {
                        context.saveBackupUser(items[position])
                        backupUser.set(items[position])
                    }
                    R.id.materialButton_change_restore_user -> {
                        context.saveRestoreUser(items[position])
                        restoreUser.set(items[position])
                    }
                }
                dismiss()
            }
            show()
        }
    }

    fun onSelectAppBtnClick(v: View) {
        v.context.startActivity(Intent(v.context, BackupAppListActivity::class.java))
    }

    fun onBackupAppBtnClick(v: View) {
        v.context.startActivity(Intent(v.context, BackupProcessingActivity::class.java).apply {
            putExtra("isMedia", false)
        })
    }

    fun onBackupMediaBtnClick(v: View) {
        v.context.startActivity(
            Intent(v.context, BackupProcessingActivity::class.java).apply {
                putExtra("isMedia", true)
            }
        )
    }

    private fun setNum() {
        val that = this
        Command.getCachedAppInfoBackupListNum().apply {
            that.appNum.set(this.appNum.toString())
            that.dataNum.set(this.dataNum.toString())
        }
    }

    private fun refresh() {
        setNum()
    }

    fun initialize() {
        refresh()
    }
}