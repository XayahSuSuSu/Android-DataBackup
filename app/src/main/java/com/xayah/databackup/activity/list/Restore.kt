package com.xayah.databackup.activity.list

import android.annotation.SuppressLint
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.adapter.AppListAdapterRestore
import com.xayah.databackup.adapter.AppListHeaderAdapterBase
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class Restore(private val mAdapter: MultiTypeAdapter) {
    var mAppInfoRestoreList: MutableList<AppInfoRestore> = mutableListOf()
    var appNumFull = true
    var dataNumFull = true

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(onInitialized: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            mAdapter.apply {
                val adapterList = mutableListOf<Any>()
                // 按照字母表排序
                mAppInfoRestoreList = Command.getCachedAppInfoRestoreList(false).apply {
                    sortWith { appInfo1, appInfo2 ->
                        val collator = Collator.getInstance(Locale.CHINA)
                        collator.getCollationKey((appInfo1 as AppInfoRestore).infoBase.appName)
                            .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).infoBase.appName))
                    }
                }
                register(
                    AppListHeaderAdapterBase(onInitialize = {
                        updateChip(it)
                    }, onChipAppClick = {
                        appNumFull = !appNumFull
                        for (i in mAppInfoRestoreList) i.infoBase.app = appNumFull
                        mAdapter.notifyDataSetChanged()
                    }, onChipDataClick = {
                        dataNumFull = !dataNumFull
                        for (i in mAppInfoRestoreList) i.infoBase.data = dataNumFull
                        mAdapter.notifyDataSetChanged()
                    })
                )
                register(AppListAdapterRestore(mAppInfoRestoreList))
                adapterList.add(0, "Header")
                adapterList.addAll(mAppInfoRestoreList)
                items = adapterList
                withContext(Dispatchers.Main) {
                    onInitialized()
                }
            }
        }
    }

    private fun updateChip(binding: AdapterAppListHeaderBinding) {
        var appNum = 0
        var dataNum = 0
        val size = mAppInfoRestoreList.size
        for (i in mAppInfoRestoreList) {
            if (i.infoBase.app) appNum++
            if (i.infoBase.data) dataNum++
        }
        appNumFull = appNum == size
        dataNumFull = dataNum == size
        binding.chipApp.isChecked = appNumFull
        binding.chipData.isChecked = dataNumFull
    }

    fun saveAppList() {
        JSON.writeJSONToFile(
            JSON.entityArrayToJsonArray(mAppInfoRestoreList as MutableList<Any>),
            Path.getAppInfoRestoreListPath()
        )
    }
}