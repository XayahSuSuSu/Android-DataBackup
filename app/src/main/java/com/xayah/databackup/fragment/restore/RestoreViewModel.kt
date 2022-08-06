package com.xayah.databackup.fragment.restore

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xayah.databackup.App
import com.xayah.databackup.activity.list.AppListActivity
import com.xayah.databackup.activity.processing.ProcessingActivity
import com.xayah.databackup.data.AppInfoBaseNum
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.MediaInfo
import com.xayah.databackup.util.*
import com.xayah.databackup.view.fastInitialize
import com.xayah.databackup.view.setLoading
import com.xayah.databackup.view.util.setWithConfirm
import kotlinx.coroutines.launch

class RestoreViewModel : ViewModel() {
    val _isInitialized by lazy {
        MutableLiveData(false)
    }
    private var isInitialized
        get() = _isInitialized.value!!
        set(value) = _isInitialized.postValue(value)

    // 是否第一次进入Fragment
    private val _isFirst by lazy {
        MutableLiveData(true)
    }
    private var isFirst
        get() = _isFirst.value!!
        set(value) = _isFirst.postValue(value)

    // 应用恢复列表
    private val _appInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<AppInfoRestore>())
    }
    private var appInfoRestoreList
        get() = _appInfoRestoreList.value!!.filter { it.infoBase.app || it.infoBase.data }
            .toMutableList()
        set(value) = _appInfoRestoreList.postValue(value)
    private val appInfoRestoreListNum: LiveData<AppInfoBaseNum> =
        Transformations.map(_appInfoRestoreList) { appInfoRestoreList ->
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in appInfoRestoreList) {
                if (i.infoBase.app) appInfoBaseNum.appNum++
                if (i.infoBase.data) appInfoBaseNum.dataNum++
            }
            appInfoBaseNum
        }
    val appNum: LiveData<String> =
        Transformations.map(appInfoRestoreListNum) { num -> num.appNum.toString() }
    val dataNum: LiveData<String> =
        Transformations.map(appInfoRestoreListNum) { num -> num.dataNum.toString() }

    // 媒体恢复列表
    private val _mediaInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<MediaInfo>())
    }
    var mediaInfoRestoreList
        get() = _mediaInfoRestoreList.value!!
        set(value) = _mediaInfoRestoreList.postValue(value)

    var backupUser = ObservableField("${GlobalString.user}0")
    var restoreUser = ObservableField("${GlobalString.user}0")

    var autoFixMultiUserContextEnable = ObservableBoolean(false)
    var onResume = {}

    init {
        onResume = {
            if (isFirst) {
                isFirst = false
            } else {
                isInitialized = false
            }
        }
        viewModelScope.launch {
            Command.checkLsZd().apply {
                autoFixMultiUserContextEnable.set(this)
                App.globalContext.saveAutoFixMultiUserContext(this)
            }
        }
    }

    fun onChangeUser(v: View) {
        viewModelScope.launch {
            val context = v.context
            val items =
                if (Bashrc.listUsers().first) Bashrc.listUsers().second.toTypedArray() else arrayOf(
                    "0"
                )
            val choice = items.indexOf(App.globalContext.readRestoreUser())

            ListPopupWindow(context).apply {
                fastInitialize(v, items, choice)
                setOnItemClickListener { _, _, position, _ ->
                    dismiss()
                    viewModelScope.launch {
                        context.saveRestoreUser(items[position])
                        restoreUser.set("${GlobalString.user}${items[position]}")
                        isInitialized = false
                    }
                }
                show()
            }
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
            viewModelScope.launch {
                Command.retrieve(Command.getCachedAppInfoRestoreActualList())
                refresh()
                that.dismiss()
                Toast.makeText(v.context, GlobalString.retrieveFinish, Toast.LENGTH_SHORT)
                    .show()
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
                    viewModelScope.launch {
                        Command.rm("${Path.getBackupDataSavePath()} ${Path.getAppInfoRestoreListPath()}")
                            .apply {
                                if (this) {
                                    Command.retrieve(Command.getCachedAppInfoRestoreActualList())
                                    refresh()
                                    Toast.makeText(
                                        context, GlobalString.success, Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context, GlobalString.failed, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        that.dismiss()
                    }
                }
            }
        }
    }

    private suspend fun loadAllList() {
        appInfoRestoreList = Loader.loadAppInfoRestoreList()
        mediaInfoRestoreList = Loader.loadMediaInfoRestoreList()
    }

    suspend fun refresh() {
        // 加载列表
        loadAllList()
        setUser()
        isInitialized = true
    }

    private fun setUser() {
        backupUser.set("${GlobalString.user}${App.globalContext.readBackupUser()}")
        restoreUser.set("${GlobalString.user}${App.globalContext.readRestoreUser()}")
    }
}