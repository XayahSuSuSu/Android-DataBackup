package com.xayah.databackup.fragment.restore

import android.content.Intent
import android.view.View
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.activity.list.AppListActivity
import com.xayah.databackup.activity.processing.ProcessingActivity
import com.xayah.databackup.util.*
import com.xayah.databackup.view.fastInitialize

class RestoreViewModel : ViewModel() {
    var backupUser = ObservableField("${GlobalString.user}${App.globalContext.readBackupUser()}")
    var restoreUser = ObservableField("${GlobalString.user}${App.globalContext.readRestoreUser()}")
    var appNum = ObservableField("0")
    var dataNum = ObservableField("0")

    fun onChangeUser(v: View) {
        val context = v.context
        val items =
            if (Bashrc.listUsers().first) Bashrc.listUsers().second.toTypedArray() else arrayOf("0")
        val choice = items.indexOf(App.globalContext.readRestoreUser())

        ListPopupWindow(context).apply {
            fastInitialize(v, items, choice)
            setOnItemClickListener { _, _, position, _ ->
                context.saveRestoreUser(items[position])
                restoreUser.set("${GlobalString.user}${items[position]}")
                refresh()
                dismiss()
            }
            show()
        }
    }

    fun onSelectAppBtnClick(v: View) {
        v.context.startActivity(Intent(v.context, AppListActivity::class.java).apply {
            putExtra("isRestore", true)
        })
    }

    fun onRestoreAppBtnClick(v: View) {
        v.context.startActivity(Intent(v.context, ProcessingActivity::class.java).apply {
            putExtra("isRestore", true)
            putExtra("isMedia", false)
        })
    }

    fun onRestoreMediaBtnClick(v: View) {
        v.context.startActivity(
            Intent(v.context, ProcessingActivity::class.java).apply {
                putExtra("isRestore", true)
                putExtra("isMedia", true)
            }
        )
    }

    private fun setNum() {
        val that = this
        Command.getCachedAppInfoRestoreListNum().apply {
            that.appNum.set(this.appNum.toString())
            that.dataNum.set(this.dataNum.toString())
        }
    }

    private fun refresh() {
        setNum()
        setUser()

    }

    private fun setUser() {
        backupUser.set("${GlobalString.user}${App.globalContext.readBackupUser()}")
        restoreUser.set("${GlobalString.user}${App.globalContext.readRestoreUser()}")
    }

    fun initialize() {
        refresh()
    }
}