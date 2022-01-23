package com.xayah.databackup.fragment.restore

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.R
import com.xayah.databackup.adapter.AppListDelegate
import com.xayah.databackup.model.app.AppEntity
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.ShellUtil

class RestoreViewModel : ViewModel() {
    var appEntityList = mutableListOf<AppEntity>()
    val adapter = MultiTypeAdapter()
    lateinit var appListDelegate: AppListDelegate

    var isInitialized = false

    fun initialize(mContext: Context, path: String) {
        appListDelegate = AppListDelegate(mContext)
        val pathUtil = PathUtil(mContext)
        if (!isInitialized) {
            appEntityList = mutableListOf()
            val appListFile = ShellUtil.cat(path + "/" + pathUtil.APP_LIST_FILE_NAME)
            for (i in appListFile) {
                val info = i.split(" ")
                if (info.size == 2) {
                    val appEntity = AppEntity(0, info[0], info[1])
                    appEntity.isSelected = !i.contains("#")
                    appEntity.isOnly = i.contains("!")
                    appEntity.appIcon = AppCompatResources.getDrawable(
                        mContext,
                        R.drawable.ic_outline_android
                    )
                    appEntityList.add(appEntity)
                }
                appListDelegate.isRestore = true
                adapter.register(appListDelegate)
                adapter.items = appEntityList
            }
            isInitialized = true
        }

    }
}