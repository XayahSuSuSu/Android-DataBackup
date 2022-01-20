package com.xayah.databackup.fragment.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.model.app.AppDatabase
import com.xayah.databackup.util.DataUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupViewModel : ViewModel() {
    val adapter = MultiTypeAdapter()

    fun initialize(mContext: Context, onInitialized: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                mContext,
                AppDatabase::class.java, "app"
            ).build()
            val appEntityList = db.appDao().getAllApps()
            adapter.register(AppListDelegate(mContext))
            for ((index, i) in appEntityList.withIndex()) {
                val (appIcon, appName, appPackage) = DataUtil.getAppInfo(mContext, i.appPackage)
                appEntityList[index].appIcon = appIcon
                appEntityList[index].appName = appName
                appEntityList[index].appPackage = appPackage
            }
            adapter.items = appEntityList
            onInitialized()
        }
    }
}