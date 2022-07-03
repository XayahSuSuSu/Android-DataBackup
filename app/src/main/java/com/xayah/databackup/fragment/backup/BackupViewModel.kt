package com.xayah.databackup.fragment.backup

import android.content.Intent
import android.os.Environment
import android.view.View
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.activity.backup.list.app.BackupAppListActivity
import com.xayah.databackup.activity.backup.processing.app.BackupProcessingActivity
import com.xayah.databackup.util.*
import com.xayah.design.adapter.PopupListAdapter
import com.xayah.design.card.StorageRadioCard
import com.xayah.design.util.getPixels
import com.xayah.design.util.measureWidth

class BackupViewModel : ViewModel() {
    var radioGroupCheckedIndex = ObservableInt(0)
    var internalStorageString = ObservableField("")
    var internalStorageValue = ObservableInt(0)
    var otgString = ObservableField(GlobalString.notPluggedIn)
    var otgValue = ObservableInt(0)
    var otgEnabled = ObservableBoolean(false)
    var backupUser = ObservableField(App.globalContext.readBackupUser())
    var restoreUser = ObservableField(App.globalContext.readRestoreUser())
    var appNum = ObservableField("0")
    var dataNum = ObservableField("0")

    private fun setOTG() {
        // 默认值
        otgString.set(GlobalString.notPluggedIn)
        otgValue.set(0)
        otgEnabled.set(false)
        // 检查OTG连接情况
        Bashrc.checkOTG().apply {
            val that = this
            if (that.first == 0) {
                val space = Bashrc.getStorageSpace(that.second)
                if (space.first) {
                    try {
                        val string = space.second
                        otgValue.set(string.split(" ").last().replace("%", "").toInt())
                        otgString.set(that.second + "/DataBackup")
                        otgEnabled.set(true)
                    } catch (e: NumberFormatException) {
                        otgString.set(GlobalString.fetchFailed)
                        e.printStackTrace()
                    }
                }
            } else if (that.first == 1) {
                radioGroupCheckedIndex.set(0)
                otgString.set(GlobalString.unsupportedFormat)
            } else {
                radioGroupCheckedIndex.set(0)
            }
        }
    }

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

    private fun setInternalStorage() {
        val path = "${Environment.getExternalStorageDirectory().path}/DataBackup"
        // 默认值
        internalStorageString.set(GlobalString.fetching)
        internalStorageValue.set(0)

        val space = Bashrc.getStorageSpace(path)
        val string = if (space.first) space.second else GlobalString.error
        internalStorageString.set(path)
        if (space.first) {
            try {
                internalStorageValue.set(string.split(" ").last().replace("%", "").toInt())
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                internalStorageString.set(GlobalString.fetchFailed)
            }
        } else {
            internalStorageString.set(GlobalString.fetchFailed)
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

    val onStorageRadioCardCheckedChangeListener: (v: StorageRadioCard, index: Int) -> Unit =
        { _, index ->
            radioGroupCheckedIndex.set(index)
            App.globalContext.saveBackupSavePath(if (index == 0) internalStorageString.get() else otgString.get())
        }

    private fun setNum() {
        val that = this
        Command.getCachedAppInfoBackupListNum().apply {
            that.appNum.set(this.appNum.toString())
            that.dataNum.set(this.dataNum.toString())
        }
    }

    private fun refresh() {
        setInternalStorage()
        setOTG()
        setNum()
    }

    fun initialize() {
        refresh()
    }
}