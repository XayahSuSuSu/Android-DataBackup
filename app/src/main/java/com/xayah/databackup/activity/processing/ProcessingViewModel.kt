package com.xayah.databackup.activity.processing

import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.data.AppInfoBase
import com.xayah.databackup.util.GlobalString

data class DataBinding(
    var appName: ObservableField<String>,
    var packageName: ObservableField<String>,
    var appVersion: ObservableField<String>,
    var appIcon: ObservableField<Drawable>,
    var size: ObservableField<String>,
    var sizeUnit: ObservableField<String>,
    var speed: ObservableField<String>,
    var speedUnit: ObservableField<String>,
    var isProcessing: ObservableBoolean,
    var btnText: ObservableField<String>,
    var btnDesc: ObservableField<String>,
    var totalTip: ObservableField<String>,
    var totalProgress: ObservableField<String>,
    var successProgress: ObservableField<String>,
    var failedProgress: ObservableField<String>,
    var progressText: ObservableField<String>,
    var progressMax: ObservableInt,
    var progress: ObservableInt,
    var isBackupApk: ObservableBoolean,
    var isBackupUser: ObservableBoolean,
    var isBackupUserDe: ObservableBoolean,
    var isBackupData: ObservableBoolean,
    var isBackupObb: ObservableBoolean,
    var processingApk: ObservableBoolean,
    var processingUser: ObservableBoolean,
    var processingUserDe: ObservableBoolean,
    var processingData: ObservableBoolean,
    var processingObb: ObservableBoolean,
    var isReady: ObservableBoolean,
    var isFinished: MutableLiveData<Boolean>,

    var onBackupClick: (v: View) -> Unit,
    var onRestoreClick: (v: View) -> Unit
)

class ProcessingViewModel : ViewModel() {
    val dataBinding = DataBinding(
        ObservableField(""),
        ObservableField(""),
        ObservableField(""),
        ObservableField<Drawable>(),
        ObservableField("0"),
        ObservableField("Mib"),
        ObservableField("0"),
        ObservableField("Mib/s"),
        ObservableBoolean(false),
        ObservableField(""),
        ObservableField(""),
        ObservableField(""),
        ObservableField(""),
        ObservableField(""),
        ObservableField(""),
        ObservableField(GlobalString.progress),
        ObservableInt(0),
        ObservableInt(0),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        ObservableBoolean(false),
        MutableLiveData(false),
        {}, {})

    var isRestore = false
    var isMedia = false
    lateinit var backup: Backup
    lateinit var restore: Restore

    val mAdapter: MultiTypeAdapter = MultiTypeAdapter()
    val mAdapterSuccess: MultiTypeAdapter = MultiTypeAdapter()
    val mAdapterFailed: MultiTypeAdapter = MultiTypeAdapter()

    // 成功列表
    val _successList by lazy {
        MutableLiveData(mutableListOf<AppInfoBase>())
    }
    var successList
        get() = _successList.value!!
        set(value) = _successList.postValue(value)

    // 失败列表
    val _failedList by lazy {
        MutableLiveData(mutableListOf<AppInfoBase>())
    }
    var failedList
        get() = _failedList.value!!
        set(value) = _failedList.postValue(value)

    fun initialize() {
        if (isRestore)
            restore = Restore(this)
        else
            backup = Backup(this)
    }

    fun onBtnClick(v: View) {
        if (isRestore)
            dataBinding.onRestoreClick(v)
        else
            dataBinding.onBackupClick(v)
    }
}