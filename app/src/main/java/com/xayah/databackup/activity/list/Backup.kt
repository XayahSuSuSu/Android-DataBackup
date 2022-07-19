package com.xayah.databackup.activity.list

import android.annotation.SuppressLint
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.App
import com.xayah.databackup.adapter.AppListAdapterBackup
import com.xayah.databackup.adapter.AppListHeaderAdapterBase
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Backup(private val mAdapter: MultiTypeAdapter) {
    private val mAppInfoBackupList = App.globalAppInfoBackupList
    var appNumFull = true
    var dataNumFull = true

    @SuppressLint("NotifyDataSetChanged")
    fun initialize(onInitialized: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            mAdapter.apply {
                val adapterList = mutableListOf<Any>()
                register(
                    AppListHeaderAdapterBase(onInitialize = {
                        updateChip(it)
                    }, onChipAppClick = {
                        appNumFull = !appNumFull
                        for (i in mAppInfoBackupList) i.infoBase.app = appNumFull
                        mAdapter.notifyDataSetChanged()
                    }, onChipDataClick = {
                        dataNumFull = !dataNumFull
                        for (i in mAppInfoBackupList) i.infoBase.data = dataNumFull
                        mAdapter.notifyDataSetChanged()
                    }, onSearchViewQueryTextChange = { newText ->
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(mAppInfoBackupList.filter {
                            it.infoBase.appName.lowercase().contains(newText.toString().lowercase())
                        })
                        items = adapterList
                        mAdapter.notifyDataSetChanged()
                    })
                )
                register(AppListAdapterBackup())
                adapterList.add(0, "Header")
                adapterList.addAll(mAppInfoBackupList)
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
        val size = mAppInfoBackupList.size
        for (i in mAppInfoBackupList) {
            if (i.infoBase.app) appNum++
            if (i.infoBase.data) dataNum++
        }
        appNumFull = appNum == size
        dataNumFull = dataNum == size
        binding.chipApp.isChecked = appNumFull
        binding.chipData.isChecked = dataNumFull
    }
}