package com.xayah.databackup.activity.processing

import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel

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
    var totalTip: ObservableField<String>,
    var totalProgress: ObservableField<String>,
    var progressMax: ObservableInt,
    var progress: ObservableInt,
    var isBackupApk: ObservableBoolean,
    var isBackupUser: ObservableBoolean,
    var isBackupData: ObservableBoolean,
    var isBackupObb: ObservableBoolean,
    var processingApk: ObservableBoolean,
    var processingUser: ObservableBoolean,
    var processingData: ObservableBoolean,
    var processingObb: ObservableBoolean,

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
        {}, {})
    var isRestore = false
    lateinit var backup: Backup
    lateinit var restore: Restore

    fun initialize(mIsRestore: Boolean, mIsMedia: Boolean) {
        isRestore = mIsRestore
        val that = this
        if (isRestore)
            restore = Restore().apply {
                initialize(that.dataBinding, mIsMedia)
            }
        else
            backup = Backup().apply {
                initialize(that.dataBinding, mIsMedia)
            }
    }

    fun onBtnClick(v: View) {
        if (isRestore)
            dataBinding.onRestoreClick(v)
        else
            dataBinding.onBackupClick(v)
    }
}