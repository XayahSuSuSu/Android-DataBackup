package com.xayah.databackup.fragment.backup

import android.view.View
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.util.*
import com.xayah.design.adapter.PopupListAdapter
import com.xayah.design.util.getPixels
import com.xayah.design.util.measureWidth

class BackupViewModel : ViewModel() {
    var internalStorageString = ObservableField("")
    var internalStorageMaxValue = 100
    var internalStorageValue = ObservableInt(0)
    var internalStorageCheck = ObservableBoolean(true)
    var otgString = ObservableField(GlobalString.notPluggedIn)
    var otgMaxValue = 100
    var otgValue = ObservableInt(0)
    var otgCheck = ObservableBoolean(false)
    var otgEnabled = ObservableBoolean(false)
    var radioGroupCheckedId = ObservableInt(R.id.materialRadioButton_internal_storage)
    var backupUser = ObservableField(App.globalContext.readBackupUser())
    var restoreUser = ObservableField(App.globalContext.readRestoreUser())
    var appNum = ObservableField("0")
    var dataNum = ObservableField("0")

    fun onRadioButtonCheckedChanged(v: View, isChecked: Boolean) {
        setOTG()
        if (isChecked) {
            radioGroupCheckedId.set(v.id)
        }
        updateRadioButton()
    }

    private fun updateRadioButton() {
        internalStorageCheck.set(radioGroupCheckedId.get() == R.id.materialRadioButton_internal_storage)
        otgCheck.set(radioGroupCheckedId.get() == R.id.materialRadioButton_otg)
    }

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
                val string = if (space.first) space.second else GlobalString.error
                otgString.set(that.second + "/DataBackup")
                if (space.first) {
                    try {
                        otgValue.set(string.split(" ").last().replace("%", "").toInt())
                        otgEnabled.set(true)
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            } else if (that.first == 1) {
                radioGroupCheckedId.set(R.id.materialRadioButton_internal_storage)
                otgString.set(GlobalString.unsupportedFormat)
            } else {
                radioGroupCheckedId.set(R.id.materialRadioButton_internal_storage)
            }
        }
        updateRadioButton()
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
                context.getPixels(com.xayah.design.R.dimen.item_header_component_size) +
                        context.getPixels(com.xayah.design.R.dimen.item_header_margin) * 2
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
        val path = App.globalContext.readBackupSavePath()
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

    private fun refresh() {
        setInternalStorage()
        setOTG()
    }

    fun initialize() {
        refresh()
    }
}