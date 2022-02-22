package com.xayah.databackup.fragment.restore

import android.content.Context
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.model.AppInfo
import com.xayah.databackup.util.DataUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RestoreViewModel : ViewModel() {
    val adapter = MultiTypeAdapter()

    var isInitialized = false

    var backupPath = ""

    fun initialize(
        mContext: Context,
        appListFile: MutableList<String>,
        appListDelegate: AppListDelegate,
    ) {
        if (!isInitialized) {
            CoroutineScope(Dispatchers.IO).launch {
                val appList: MutableList<AppInfo> = mutableListOf()
                for (i in appListFile) {
                    try {
                        val info = i.split(" ")
                        if (!info[0].contains("#不需要")) {
                            val appInfo = AppInfo(
                                info[0].replace("[#\\/:*?\"<>|!]".toRegex(), ""),
                                info[1],
                                info[2],
                                i.contains("!"),
                                !i.contains("#")
                            )
                            val (appIcon, _, _) = DataUtil.getAppInfo(
                                mContext,
                                appInfo.appPackage
                            )
                            appInfo.appIcon = appIcon
                            appList.add(appInfo)
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    }
                }

                adapter.register(appListDelegate)
                appListDelegate.isAttached = true
                adapter.items = appList
            }
            isInitialized = true
        }

    }
}