package com.xayah.databackup.fragment.restore

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.activity.list.AppListActivity
import com.xayah.databackup.activity.processing.ProcessingActivity
import com.xayah.databackup.util.*
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.setLoading
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreViewModel : ViewModel() {
    var backupUser = ObservableField("${GlobalString.user}${App.globalContext.readBackupUser()}")
    var restoreUser = ObservableField("${GlobalString.user}${App.globalContext.readRestoreUser()}")
    var appNum = ObservableField("0")
    var dataNum = ObservableField("0")

    fun onChangeUser(v: View, callback: () -> Unit) {
        val context = v.context
        val items =
            if (Bashrc.listUsers().first) Bashrc.listUsers().second.toTypedArray() else arrayOf("0")
        val choice = items.indexOf(App.globalContext.readRestoreUser())

        ListPopupWindow(context).apply {
            fastInitialize(v, items, choice)
            setOnItemClickListener { _, _, position, _ ->
                dismiss()
                BottomSheetDialog(v.context).apply {
                    setLoading()
                    CoroutineScope(Dispatchers.IO).launch {
                        App.saveGlobalList()
                        context.saveRestoreUser(items[position])
                        restoreUser.set("${GlobalString.user}${items[position]}")
                        App.initializeGlobalList()
                        withContext(Dispatchers.Main) {
                            callback()
                            refresh()
                            dismiss()
                        }
                    }
                }
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
        v.context.startActivity(Intent(v.context, ProcessingActivity::class.java).apply {
            putExtra("isRestore", true)
            putExtra("isMedia", true)
        })
    }

    fun onFixBtnClick(v: View) {
        BottomSheetDialog(v.context).apply {
            setLoading()
            val that = this
            CoroutineScope(Dispatchers.IO).launch {
                Command.retrieve(Command.getCachedAppInfoRestoreActualList())
                App.initializeGlobalList()
                withContext(Dispatchers.Main) {
                    that.dismiss()
                    Toast.makeText(v.context, GlobalString.retrieveFinish, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun onRestoreAppClearBtnClick(v: View) {
        val context = v.context
        MaterialAlertDialogBuilder(context).apply {
            setWithConfirm("${GlobalString.confirmRemoveAllAppAndDataThatBackedUp}${GlobalString.symbolQuestion}") {
                BottomSheetDialog(v.context).apply {
                    setLoading()
                    val that = this
                    CoroutineScope(Dispatchers.IO).launch {
                        Command.rm("${Path.getBackupDataSavePath()} ${Path.getAppInfoRestoreListPath()}")
                            .apply {
                                if (this) {
                                    Command.retrieve(Command.getCachedAppInfoRestoreActualList())
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context, GlobalString.success, Toast.LENGTH_SHORT
                                        ).show()
                                        refresh()
                                    }
                                } else withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context, GlobalString.failed, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        withContext(Dispatchers.Main) {
                            that.dismiss()
                        }
                    }
                }
            }
        }
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