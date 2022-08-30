package com.xayah.databackup.activity.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.adapter.AppListAdapterBackup
import com.xayah.databackup.adapter.AppListHeaderAdapterBase
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.databinding.AdapterAppListHeaderBinding
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class Backup(private val viewModel: AppListViewModel) {
    private val _appInfoBackupList by lazy {
        MutableLiveData(mutableListOf<AppInfoBackup>())
    }
    private var appInfoBackupList
        get() = _appInfoBackupList.value!!
        set(value) = _appInfoBackupList.postValue(value)

    private var appNumFull = true
    private var dataNumFull = true

    init {
        viewModel.onPause = suspend {
            if (!viewModel.isBack)
                JSON.saveAppInfoBackupList(appInfoBackupList)
        }
        viewModel.onResume = suspend {
            if (viewModel.isFirst) {
                viewModel.isFirst = false
            } else {
                loadAppInfoBackupList()
                viewModel.isInitialized = false
            }
        }
        viewModel.viewModelScope.launch {
            // 载入备份列表
            loadAppInfoBackupList()
            viewModel.mAdapter.apply {
                val adapterList = mutableListOf<Any>()
                register(
                    AppListHeaderAdapterBase(
                        onInitialize = {
                            updateChip(it)
                        }, onChipAppClick = {
                            appNumFull = !appNumFull
                            for (i in appInfoBackupList) i.infoBase.app = appNumFull
                            viewModel.mAdapter.notifyDataSetChanged()
                        }, onChipDataClick = {
                            dataNumFull = !dataNumFull
                            for (i in appInfoBackupList) i.infoBase.data = dataNumFull
                            viewModel.mAdapter.notifyDataSetChanged()
                        }, onSearchViewQueryTextChange = { newText ->
                            adapterList.clear()
                            adapterList.add(0, "Header")
                            adapterList.addAll(appInfoBackupList.filter {
                                it.infoBase.appName.lowercase()
                                    .contains(newText.toString().lowercase())
                            })
                            items = adapterList
                            viewModel.mAdapter.notifyDataSetChanged()
                        }, onNoneBtnClick = {
                            adapterList.clear()
                            adapterList.add(0, "Header")
                            adapterList.addAll(appInfoBackupList)
                            items = adapterList
                            viewModel.mAdapter.notifyDataSetChanged()
                        }, onSelectedBtnClick = {
                            adapterList.clear()
                            adapterList.add(0, "Header")
                            adapterList.addAll(appInfoBackupList.filter { it.infoBase.app || it.infoBase.data })
                            items = adapterList
                            viewModel.mAdapter.notifyDataSetChanged()
                        }, onNotSelectedBtnClick = {
                            adapterList.clear()
                            adapterList.add(0, "Header")
                            adapterList.addAll(appInfoBackupList.filter { !it.infoBase.app && !it.infoBase.data })
                            items = adapterList
                            viewModel.mAdapter.notifyDataSetChanged()
                        })
                )
                register(AppListAdapterBackup())
                adapterList.add(0, "Header")
                adapterList.addAll(appInfoBackupList)
                items = adapterList
                viewModel.isInitialized = true
            }
        }
    }

    private fun updateChip(binding: AdapterAppListHeaderBinding) {
        var appNum = 0
        var dataNum = 0
        val size = appInfoBackupList.size
        for (i in appInfoBackupList) {
            if (i.infoBase.app) appNum++
            if (i.infoBase.data) dataNum++
        }
        appNumFull = appNum == size
        dataNumFull = dataNum == size
        binding.chipApp.isChecked = appNumFull
        binding.chipData.isChecked = dataNumFull
    }

    private suspend fun loadAppInfoBackupList() {
        withContext(Dispatchers.IO) {
            appInfoBackupList = Command.getAppInfoBackupList(App.globalContext).apply {
                sortWith { appInfo1, appInfo2 ->
                    val collator = Collator.getInstance(Locale.CHINA)
                    collator.getCollationKey((appInfo1 as AppInfoBackup).infoBase.appName)
                        .compareTo(collator.getCollationKey((appInfo2 as AppInfoBackup).infoBase.appName))
                }
            }
        }
    }
}