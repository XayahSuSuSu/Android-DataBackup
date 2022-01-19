package com.xayah.databackup.fragment.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.model.app.AppEntity
import com.xayah.databackup.util.DataUtil
import com.xayah.databackup.util.ShellUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel : ViewModel() {
    val adapter = MultiTypeAdapter()

    fun initialize(mContext: Context, appListPath: String, onInitialized: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val appListFile = ShellUtil.cat(appListPath)
            val appEntityList = mutableListOf<AppEntity>()
            for (i in appListFile) {
                val info = i.split(" ")
                if (info.size == 2) {
                    val (appIcon, appName, appPackage) = DataUtil.getAppInfo(
                        mContext,
                        info[1]
                    )
                    val appEntity = AppEntity(0, appName, appPackage)
                    appEntity.appIcon = appIcon
                    appEntityList.add(appEntity)
                }
            }
            adapter.register(AppListDelegate(mContext))
            adapter.items = appEntityList
            onInitialized()
        }
    }
}