package com.xayah.databackup.activity.list

import android.content.Intent
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.App
import com.xayah.databackup.activity.processing.ProcessingActivity
import com.xayah.databackup.adapter.AppListAdapterBackup
import com.xayah.databackup.data.*
import com.xayah.databackup.util.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class AppListBackupActivity : AppListBaseActivity() {
    companion object {
        const val TAG = "AppListBackupActivity"
    }

    // 是否第一次访问
    private var isFirst = true

    // 经过过滤或排序后的应用列表
    private val mAppInfoList by lazy {
        MutableStateFlow(mutableListOf<AppInfoBackup>())
    }

    private lateinit var tabLayout: TabLayout

    override fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter) {
        multiTypeAdapter.register(
            AppListAdapterBackup(
                onChipClick = { updateBadges(App.appInfoBackupListNum) }
            )
        )
    }

    override fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any> {
        val adapterList = mutableListOf<Any>()
        when (pref.type) {
            AppListType.InstalledApp -> {
                // 安装应用
                adapterList.addAll(mAppInfoList.value.filter { !it.infoBase.isSystemApp })
            }
            AppListType.SystemApp -> {
                // 系统应用
                adapterList.addAll(mAppInfoList.value.filter { it.infoBase.isSystemApp })
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
            mAppInfoList.emit(App.appInfoBackupList.value.apply {
                when (pref.sort) {
                    AppListSort.AlphabetAscending -> {
                        sortWith { appInfo1, appInfo2 ->
                            val collator = Collator.getInstance(Locale.CHINA)
                            collator.getCollationKey((appInfo1 as AppInfoBackup).infoBase.appName)
                                .compareTo(collator.getCollationKey((appInfo2 as AppInfoBackup).infoBase.appName))
                        }
                    }
                    AppListSort.AlphabetDescending -> {
                        sortWith { appInfo1, appInfo2 ->
                            val collator = Collator.getInstance(Locale.CHINA)
                            collator.getCollationKey((appInfo2 as AppInfoBackup).infoBase.appName)
                                .compareTo(collator.getCollationKey((appInfo1 as AppInfoBackup).infoBase.appName))
                        }
                    }
                    AppListSort.FirstInstallTimeAscending -> {
                        sortBy { it.infoBase.firstInstallTime }
                    }
                    AppListSort.FirstInstallTimeDescending -> {
                        sortByDescending { it.infoBase.firstInstallTime }
                    }
                }
            })
            when (pref.filter) {
                AppListFilter.None -> {}
                AppListFilter.Selected -> {
                    mAppInfoList.emit(mAppInfoList.value.filter { it.infoBase.app || it.infoBase.data }
                        .toMutableList())
                }
                AppListFilter.NotSelected -> {
                    mAppInfoList.emit(mAppInfoList.value.filter { !it.infoBase.app && !it.infoBase.data }
                        .toMutableList())
                }
            }
            val keyWord = pref.searchKeyWord
            mAppInfoList.emit(mAppInfoList.value.filter {
                it.infoBase.appName.lowercase().contains(keyWord.lowercase()) ||
                        it.infoBase.packageName.lowercase().contains(keyWord.lowercase())
            }.toMutableList())

            // 计算已选中应用数量并应用Badges
            updateBadges(App.appInfoBackupListNum)
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
            JSON.saveAppInfoBackupList(App.appInfoBackupList.value)
        }
    }

    override fun onFloatingActionButtonClick(l: () -> Unit) {
        startActivity(Intent(this, ProcessingActivity::class.java).apply {
            putExtra("isRestore", false)
            putExtra("isMedia", false)
        })
    }
}

