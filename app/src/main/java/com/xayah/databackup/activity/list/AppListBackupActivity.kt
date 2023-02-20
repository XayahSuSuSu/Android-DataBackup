package com.xayah.databackup.activity.list

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.adapter.AppListAdapterBackup
import com.xayah.databackup.compose.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.data.*
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.GsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class AppListBackupActivity : AppListBaseActivity() {
    companion object {
        const val TAG = "AppListBackupActivity"
    }

    /**
     * 全局单例对象
     */
    private val globalObject = GlobalObject.getInstance()

    // 经过过滤或排序后的应用列表
    private val mAppInfoBackupList by lazy {
        MutableStateFlow(mutableListOf<AppInfoBackup>())
    }

    private lateinit var tabLayout: TabLayout

    override fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter) {
        multiTypeAdapter.register(
            AppListAdapterBackup(
                onChipClick = { updateBadges(globalObject.appInfoBackupMapNum) }
            )
        )
    }

    override fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any> {
        val adapterList = mutableListOf<Any>()
        when (pref.type) {
            AppListType.InstalledApp -> {
                // 安装应用
                val appList = mAppInfoBackupList.value.filter { !it.detailBase.isSystemApp }
                adapterList.addAll(appList)
                when (pref.installedAppSelection) {
                    AppListSelection.App -> {
                        appList.forEach { it.detailBackup.selectApp = true }
                    }
                    AppListSelection.AppReverse -> {
                        appList.forEach { it.detailBackup.selectApp = false }
                    }
                    AppListSelection.All -> {
                        appList.forEach {
                            it.detailBackup.selectApp = true
                            it.detailBackup.selectData = true
                        }
                    }
                    AppListSelection.AllReverse -> {
                        appList.forEach {
                            it.detailBackup.selectApp = false
                            it.detailBackup.selectData = false
                        }
                    }
                    else -> {}
                }
            }
            AppListType.SystemApp -> {
                // 系统应用
                val appList = mAppInfoBackupList.value.filter { it.detailBase.isSystemApp }
                adapterList.addAll(appList)
                when (pref.systemAppSelection) {
                    AppListSelection.App -> {
                        appList.forEach { it.detailBackup.selectApp = true }
                    }
                    AppListSelection.AppReverse -> {
                        appList.forEach { it.detailBackup.selectApp = false }
                    }
                    AppListSelection.All -> {
                        appList.forEach {
                            it.detailBackup.selectApp = true
                            it.detailBackup.selectData = true
                        }
                    }
                    AppListSelection.AllReverse -> {
                        appList.forEach {
                            it.detailBackup.selectApp = false
                            it.detailBackup.selectData = false
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
            if (GlobalObject.getInstance().appInfoBackupMap.value.isEmpty()) {
                GlobalObject.getInstance().appInfoBackupMap.emit(Command.getAppInfoBackupMap())
            }
            mAppInfoBackupList.emit(
                GlobalObject.getInstance().appInfoBackupMap.value.values.toList()
                    .filter { it.isOnThisDevice }.toMutableList()
            )

            mAppInfoBackupList.emit(mAppInfoBackupList.value.apply {
                when (pref.sort) {
                    AppListSort.AlphabetAscending -> {
                        sortWith { appInfo1, appInfo2 ->
                            if (appInfo1 == null && appInfo2 == null) {
                                0
                            } else if (appInfo1 == null) {
                                -1
                            } else if (appInfo2 == null) {
                                1
                            } else {
                                val collator = Collator.getInstance(Locale.CHINA)
                                collator.getCollationKey((appInfo1 as AppInfoBackup).detailBase.appName)
                                    .compareTo(collator.getCollationKey((appInfo2 as AppInfoBackup).detailBase.appName))
                            }
                        }
                    }
                    AppListSort.AlphabetDescending -> {
                        sortWith { appInfo1, appInfo2 ->
                            if (appInfo1 == null && appInfo2 == null) {
                                0
                            } else if (appInfo1 == null) {
                                -1
                            } else if (appInfo2 == null) {
                                1
                            } else {
                                val collator = Collator.getInstance(Locale.CHINA)
                                collator.getCollationKey((appInfo2 as AppInfoBackup).detailBase.appName)
                                    .compareTo(collator.getCollationKey((appInfo1 as AppInfoBackup).detailBase.appName))
                            }
                        }
                    }
                    AppListSort.FirstInstallTimeAscending -> {
                        sortBy { it.firstInstallTime }
                    }
                    AppListSort.FirstInstallTimeDescending -> {
                        sortByDescending { it.firstInstallTime }
                    }
                    AppListSort.DataSizeAscending -> {
                        sortBy { it.storageStats.sizeBytes }
                    }
                    AppListSort.DataSizeDescending -> {
                        sortByDescending { it.storageStats.sizeBytes }
                    }
                }
            })

            when (pref.filter) {
                AppListFilter.None -> {}
                AppListFilter.Selected -> {
                    mAppInfoBackupList.emit(mAppInfoBackupList.value.filter { it.detailBackup.selectApp || it.detailBackup.selectData }
                        .toMutableList())
                }
                AppListFilter.NotSelected -> {
                    mAppInfoBackupList.emit(mAppInfoBackupList.value.filter { !it.detailBackup.selectApp && !it.detailBackup.selectData }
                        .toMutableList())
                }
            }

            val keyWord = pref.searchKeyWord
            mAppInfoBackupList.emit(mAppInfoBackupList.value.filter {
                it.detailBase.appName.lowercase().contains(keyWord.lowercase()) ||
                        it.detailBase.packageName.lowercase().contains(keyWord.lowercase())
            }.toMutableList())

            // 计算已选中应用数量并应用Badges
            updateBadges(globalObject.appInfoBackupMapNum)
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
        GsonUtil.saveAppInfoBackupMapToFile(GlobalObject.getInstance().appInfoBackupMap.value)
    }

    @ExperimentalMaterial3Api
    override fun onFloatingActionButtonClick(l: () -> Unit) {
        startActivity(Intent(this, ProcessingActivity::class.java).apply {
            putExtra(TypeActivityTag, TypeBackupApp)
        })
    }
}

