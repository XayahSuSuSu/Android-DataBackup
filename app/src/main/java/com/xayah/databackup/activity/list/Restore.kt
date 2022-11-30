package com.xayah.databackup.activity.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.AppListAdapterRestore
import com.xayah.databackup.adapter.AppListHeaderAdapterBase
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.JSON
import com.xayah.databackup.util.readRestoreAppLoadType
import com.xayah.databackup.util.saveRestoreAppLoadType
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
        viewModel.onPause = suspend {
            if (!viewModel.isBack)
                JSON.saveAppInfoRestoreList(appInfoRestoreList)
        }
        viewModel.onResume = suspend {
            if (viewModel.isFirst) {
                viewModel.isFirst = false
            } else {
                loadAppInfoRestoreList()
                viewModel.isInitialized = false
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
                        for (i in appInfoRestoreList) if (i.hasApp) i.infoBase.app = appNumFull
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onChipDataClick = {
                        dataNumFull = !dataNumFull
                        for (i in appInfoRestoreList) if (i.hasData) i.infoBase.data = dataNumFull
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onSearchViewQueryTextChange = { newText ->
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(appInfoRestoreList.filter {
                            it.infoBase.appName.lowercase().contains(
                                newText.toString().lowercase()
                            ) || it.infoBase.packageName.lowercase().contains(
                                newText.toString().lowercase()
                            )
                        })
                        items = adapterList
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onNoneBtnClick = {
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(appInfoRestoreList)
                        items = adapterList
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onSelectedBtnClick = {
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(appInfoRestoreList.filter { it.infoBase.app || it.infoBase.data })
                        items = adapterList
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, onNotSelectedBtnClick = {
                        adapterList.clear()
                        adapterList.add(0, "Header")
                        adapterList.addAll(appInfoRestoreList.filter { !it.infoBase.app && !it.infoBase.data })
                        items = adapterList
                        viewModel.mAdapter.notifyDataSetChanged()
                    }, showAppLoadType = true,
                        onInstalledAppBtnClick = {
                            viewModel.viewModelScope.launch {
                                App.globalContext.saveRestoreAppLoadType(0)
                                loadAppInfoRestoreList()
                                viewModel.isInitialized = false
                            }
                        }, onSystemAppBtnClick = {
                            viewModel.viewModelScope.launch {
                                App.globalContext.saveRestoreAppLoadType(1)
                                loadAppInfoRestoreList()
                                viewModel.isInitialized = false
                            }
                        }, onAllAppBtnClick = {
                            viewModel.viewModelScope.launch {
                                App.globalContext.saveRestoreAppLoadType(2)
                                loadAppInfoRestoreList()
                                viewModel.isInitialized = false
                            }
                        }, onDefAppLoadType = { App.globalContext.readRestoreAppLoadType() }
                    )
                )
                register(AppListAdapterRestore(appInfoRestoreList))
                adapterList.add(0, "Header")
                when (App.globalContext.readRestoreAppLoadType()) {
                    0 -> {
                        // 安装应用
                        adapterList.addAll(appInfoRestoreList.filter { !it.infoBase.isSystemApp })
                    }
                    1 -> {
                        // 系统应用
                        adapterList.addAll(appInfoRestoreList.filter { it.infoBase.isSystemApp })
                    }
                    2 -> {
                        // 全部应用
                        adapterList.addAll(appInfoRestoreList)
                    }
                }
                items = adapterList
                viewModel.isInitialized = true
            }
        }
    }

    private fun updateChip(binding: AdapterAppListHeaderBinding) {
        var appNum = 0
        var hasAppNum = 0
        var dataNum = 0
        var hasDataNum = 0
        val size = appInfoRestoreList.size
        for (i in appInfoRestoreList) {
            if (i.infoBase.app) appNum++
            if (i.hasApp) hasAppNum++
            if (i.infoBase.data) dataNum++
            if (i.hasData) hasDataNum++
        }
        appNumFull = appNum == hasAppNum
        dataNumFull = dataNum == hasDataNum
        binding.chipApp.isChecked = appNumFull
        binding.chipData.isChecked = dataNumFull
    }

    private suspend fun loadAppInfoRestoreList() {
        withContext(Dispatchers.IO) {
            appInfoRestoreList = Command.getCachedAppInfoRestoreActualList().apply {
                sortWith { appInfo1, appInfo2 ->
                    val collator = Collator.getInstance(Locale.CHINA)
                    collator.getCollationKey((appInfo1 as AppInfoRestore).infoBase.appName)
                        .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).infoBase.appName))
                }
            }
        }
    }
}