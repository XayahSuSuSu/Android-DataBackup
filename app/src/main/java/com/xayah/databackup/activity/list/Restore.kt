package com.xayah.databackup.activity.list

import android.annotation.SuppressLint
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.App
import com.xayah.databackup.adapter.AppListAdapterRestore
import com.xayah.databackup.adapter.AppListHeaderAdapterBase
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Restore(private val mAdapter: MultiTypeAdapter) {
    private val mAppInfoRestoreList = App.globalAppInfoRestoreList
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
                        for (i in mAppInfoRestoreList) i.infoBase.app = appNumFull
                        mAdapter.notifyDataSetChanged()
                    }, onChipDataClick = {
                        dataNumFull = !dataNumFull
                        for (i in mAppInfoRestoreList) i.infoBase.data = dataNumFull
                        mAdapter.notifyDataSetChanged()
                    }, onSearchViewQueryTextChange = { newText ->
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(mAppInfoRestoreList.filter {
                            it.infoBase.appName.lowercase().contains(newText.toString().lowercase())
                        })
                        items = adapterList
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
}