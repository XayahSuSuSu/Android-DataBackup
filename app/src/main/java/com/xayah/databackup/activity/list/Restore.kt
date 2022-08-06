package com.xayah.databackup.activity.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.adapter.AppListAdapterRestore
import com.xayah.databackup.adapter.AppListHeaderAdapterBase
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class Restore(private val viewModel: AppListViewModel) {
    private val _appInfoRestoreList by lazy {
        MutableLiveData(mutableListOf<AppInfoRestore>())
    }
    private var appInfoRestoreList
        get() = _appInfoRestoreList.value!!
        set(value) = _appInfoRestoreList.postValue(value)

    private var appNumFull = true
    private var dataNumFull = true

    init {
        viewModel.onPause = {
            viewModel.viewModelScope.launch {
                JSON.saveAppInfoRestoreList(appInfoRestoreList)
            }
        }
        viewModel.onResume = {
            if (viewModel.isFirst) {
                viewModel.isFirst = false
            } else {
                viewModel.viewModelScope.launch {
                    loadAppInfoRestoreList()
                    viewModel.isInitialized = false
                }
            }
        }
        viewModel.viewModelScope.launch {
            // 载入恢复列表
            loadAppInfoRestoreList()
            viewModel.mAdapter.apply {
                val adapterList = mutableListOf<Any>()
                register(
                    AppListHeaderAdapterBase(onInitialize = {
                        updateChip(it)
                    }, onChipAppClick = {
                        appNumFull = !appNumFull
                        for (i in appInfoRestoreList) i.infoBase.app = appNumFull
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onChipDataClick = {
                        dataNumFull = !dataNumFull
                        for (i in appInfoRestoreList) i.infoBase.data = dataNumFull
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onSearchViewQueryTextChange = { newText ->
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(appInfoRestoreList.filter {
                            it.infoBase.appName.lowercase().contains(newText.toString().lowercase())
                        })
                        items = adapterList
                        viewModel.mAdapter.notifyDataSetChanged()
                    })
                )
                register(AppListAdapterRestore(appInfoRestoreList))
                adapterList.add(0, "Header")
                adapterList.addAll(appInfoRestoreList)
                items = adapterList
                viewModel.isInitialized = true
            }
        }
    }

    private fun updateChip(binding: AdapterAppListHeaderBinding) {
        var appNum = 0
        var dataNum = 0
        val size = appInfoRestoreList.size
        for (i in appInfoRestoreList) {
            if (i.infoBase.app) appNum++
            if (i.infoBase.data) dataNum++
        }
        appNumFull = appNum == size
        dataNumFull = dataNum == size
        binding.chipApp.isChecked = appNumFull
        binding.chipData.isChecked = dataNumFull
    }

    private suspend fun loadAppInfoRestoreList() {
        withContext(Dispatchers.IO) {
            appInfoRestoreList = Command.getCachedAppInfoRestoreList().apply {
                sortWith { appInfo1, appInfo2 ->
                    val collator = Collator.getInstance(Locale.CHINA)
                    collator.getCollationKey((appInfo1 as AppInfoRestore).infoBase.appName)
                        .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).infoBase.appName))
                }
            }
        }
    }
}