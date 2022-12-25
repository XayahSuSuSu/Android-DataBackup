package com.xayah.databackup.activity.list

import android.content.Intent
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.App
import com.xayah.databackup.activity.processing.ProcessingActivity
import com.xayah.databackup.adapter.AppListAdapterRestore
import com.xayah.databackup.data.*
import com.xayah.databackup.util.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class AppListRestoreActivity : AppListBaseActivity() {
    companion object {
        const val TAG = "AppListRestoreActivity"
    }

    // 是否第一次访问
    private var isFirst = true

    // 经过过滤或排序后的应用列表
    private val mAppInfoList by lazy {
        MutableStateFlow(mutableListOf<AppInfo>())
    }

    private lateinit var tabLayout: TabLayout

    override fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter) {
        multiTypeAdapter.register(
            AppListAdapterRestore(
                onChipClick = { updateBadges(App.appInfoRestoreListNum) },
            )
        )
    }

    override fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any> {
        val adapterList = mutableListOf<Any>()
        when (pref.type) {
            AppListType.InstalledApp -> {
                // 安装应用
                val appList = mAppInfoList.value.filter { !it.isSystemApp }
                adapterList.addAll(appList)
                when (pref.installedAppSelection) {
                    AppListSelection.App -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].app =
                                true
                        }
                    }
                    AppListSelection.AppReverse -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].app =
                                false
                        }
                    }
                    AppListSelection.All -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) {
                                it.restoreList[it.restoreIndex].app = true
                                it.restoreList[it.restoreIndex].data = true
                            }
                        }
                    }
                    AppListSelection.AllReverse -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) {
                                it.restoreList[it.restoreIndex].app = false
                                it.restoreList[it.restoreIndex].data = false
                            }
                        }
                    }
                    else -> {}
                }
            }
            AppListType.SystemApp -> {
                // 系统应用
                val appList = mAppInfoList.value.filter { it.isSystemApp }
                adapterList.addAll(appList)
                when (pref.systemAppSelection) {
                    AppListSelection.App -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].app =
                                true
                        }
                    }
                    AppListSelection.AppReverse -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].app =
                                false
                        }
                    }
                    AppListSelection.All -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) {
                                it.restoreList[it.restoreIndex].app = true
                                it.restoreList[it.restoreIndex].data = true
                            }

                        }
                    }
                    AppListSelection.AllReverse -> {
                        appList.forEach {
                            if (it.restoreList.isNotEmpty()) {
                                it.restoreList[it.restoreIndex].app = false
                                it.restoreList[it.restoreIndex].data = false
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        return adapterList
    }

    override suspend fun refreshList(pref: AppListPreferences) {
        withContext(Dispatchers.IO) {
            if (isFirst) {
                App.loadList()
                isFirst = false
            }
            mAppInfoList.emit(App.appInfoList.value.filter { it.restoreList.isNotEmpty() }
                .toMutableList())

            mAppInfoList.emit(mAppInfoList.value.apply {
                when (pref.sort) {
                    AppListSort.AlphabetAscending -> {
                        sortWith { appInfo1, appInfo2 ->
                            val collator = Collator.getInstance(Locale.CHINA)
                            collator.getCollationKey((appInfo1 as AppInfo).appName)
                                .compareTo(collator.getCollationKey((appInfo2 as AppInfo).appName))
                        }
                    }
                    AppListSort.AlphabetDescending -> {
                        sortWith { appInfo1, appInfo2 ->
                            val collator = Collator.getInstance(Locale.CHINA)
                            collator.getCollationKey((appInfo2 as AppInfo).appName)
                                .compareTo(collator.getCollationKey((appInfo1 as AppInfo).appName))
                        }
                    }
                    AppListSort.FirstInstallTimeAscending -> {
                        sortBy { it.firstInstallTime }
                    }
                    AppListSort.FirstInstallTimeDescending -> {
                        sortByDescending { it.firstInstallTime }
                    }
                }
            })

            when (pref.filter) {
                AppListFilter.None -> {}
                AppListFilter.Selected -> {
                    mAppInfoList.emit(mAppInfoList.value.filter { if (it.restoreList.isNotEmpty()) it.restoreList[it.restoreIndex].app || it.restoreList[it.restoreIndex].data else false }
                        .toMutableList())
                }
                AppListFilter.NotSelected -> {
                    mAppInfoList.emit(mAppInfoList.value.filter { if (it.restoreList.isNotEmpty()) !it.restoreList[it.restoreIndex].app && !it.restoreList[it.restoreIndex].data else false }
                        .toMutableList())
                }
            }

            val keyWord = pref.searchKeyWord
            mAppInfoList.emit(mAppInfoList.value.filter {
                it.appName.lowercase().contains(keyWord.lowercase()) ||
                        it.packageName.lowercase().contains(keyWord.lowercase())
            }.toMutableList())

            // 计算已选中应用数量并应用Badges
            updateBadges(App.appInfoRestoreListNum)
        }
    }

    override fun setTabLayout(tabLayout: TabLayout) {
        this.tabLayout = tabLayout
    }

    private fun updateBadges(appInfoListNum: AppInfoListSelectedNum) {
        tabLayout.getTabAt(0)?.orCreateBadge?.apply {
            // 安装应用
            number = appInfoListNum.installed
        }
        tabLayout.getTabAt(1)?.orCreateBadge?.apply {
            // 系统应用
            number = appInfoListNum.system
        }
    }

    override suspend fun onSave() {
        withContext(Dispatchers.IO) {
            JSON.saveAppInfoList(App.appInfoList.value)
        }
    }

    override fun onFloatingActionButtonClick(l: () -> Unit) {
        startActivity(Intent(this, ProcessingActivity::class.java).apply {
            putExtra("isRestore", true)
            putExtra("isMedia", false)
        })
    }
}

