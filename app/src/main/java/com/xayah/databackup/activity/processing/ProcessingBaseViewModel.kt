package com.xayah.databackup.activity.processing

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.data.*
import com.xayah.databackup.util.GlobalString
import kotlinx.coroutines.flow.MutableStateFlow


class ProcessingBaseViewModel : ViewModel() {
    // 适配器
    val mAdapter: MultiTypeAdapter = MultiTypeAdapter()
    val mAdapterSuccess: MultiTypeAdapter = MultiTypeAdapter()
    val mAdapterFailed: MultiTypeAdapter = MultiTypeAdapter()

    // 状态相关
    var appName: ObservableField<String> = ObservableField("")
    var packageName: ObservableField<String> = ObservableField("")
    var appVersion: ObservableField<String> = ObservableField("")
    var appIcon: ObservableField<Drawable> = ObservableField<Drawable>()
    var isProcessing: ObservableBoolean = ObservableBoolean(false)
    var btnText: ObservableField<String> = ObservableField("")
    var btnDesc: ObservableField<String> = ObservableField("")
    var totalTip: ObservableField<String> = ObservableField("")
    var totalProgress: ObservableField<String> = ObservableField("")
    var successProgress: ObservableField<String> = ObservableField("")
    var failedProgress: ObservableField<String> = ObservableField("")
    var progressText: ObservableField<String> = ObservableField(GlobalString.progress)
    var progressMax: ObservableInt = ObservableInt(0)
    var progress: ObservableInt = ObservableInt(0)
    var isReady: ObservableBoolean = ObservableBoolean(false)
    var isFinished: MutableLiveData<Boolean> = MutableLiveData(false)

    // 成功列表
    val successList by lazy {
        MutableStateFlow(mutableListOf<ProcessingTask>())
    }
    val successNum
        get() = successList.value.size

    // 失败列表
    val failedList by lazy {
        MutableStateFlow(mutableListOf<ProcessingTask>())
    }
    val failedNum
        get() = failedList.value.size

    // 备份过程
    var apkNeedProcessing: ObservableBoolean = ObservableBoolean(false)
    var apkIsProcessing: ObservableBoolean = ObservableBoolean(false)
    var apkType: ObservableField<String> = ObservableField(ProcessingItemTypeAPK)
    var apkTitle: ObservableField<String> = ObservableField(GlobalString.ready)
    var apkSubtitle: ObservableField<String> = ObservableField(GlobalString.pleaseWait)

    var userNeedProcessing: ObservableBoolean = ObservableBoolean(false)
    var userIsProcessing: ObservableBoolean = ObservableBoolean(false)
    var userType: ObservableField<String> = ObservableField(ProcessingItemTypeUSER)
    var userTitle: ObservableField<String> = ObservableField(GlobalString.ready)
    var userSubtitle: ObservableField<String> = ObservableField(GlobalString.pleaseWait)

    var userDeNeedProcessing: ObservableBoolean = ObservableBoolean(false)
    var userDeIsProcessing: ObservableBoolean = ObservableBoolean(false)
    var userDeType: ObservableField<String> = ObservableField(ProcessingItemTypeUSERDE)
    var userDeTitle: ObservableField<String> = ObservableField(GlobalString.ready)
    var userDeSubtitle: ObservableField<String> = ObservableField(GlobalString.pleaseWait)

    var dataNeedProcessing: ObservableBoolean = ObservableBoolean(false)
    var dataIsProcessing: ObservableBoolean = ObservableBoolean(false)
    var dataType: ObservableField<String> = ObservableField(ProcessingItemTypeDATA)
    var dataTitle: ObservableField<String> = ObservableField(GlobalString.ready)
    var dataSubtitle: ObservableField<String> = ObservableField(GlobalString.pleaseWait)

    var obbNeedProcessing: ObservableBoolean = ObservableBoolean(false)
    var obbIsProcessing: ObservableBoolean = ObservableBoolean(false)
    var obbType: ObservableField<String> = ObservableField(ProcessingItemTypeOBB)
    var obbTitle: ObservableField<String> = ObservableField(GlobalString.ready)
    var obbSubtitle: ObservableField<String> = ObservableField(GlobalString.pleaseWait)
}