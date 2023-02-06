package com.xayah.databackup.activity.list

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.drakeet.multitype.MultiTypeAdapter
import com.google.android.material.tabs.TabLayout
import com.xayah.databackup.adapter.AppListAdapterRestore
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

class AppListRestoreActivity : AppListBaseActivity() {
    companion object {
        const val TAG = "AppListRestoreActivity"
    }

    /**
     * 全局单例对象
     */
    private val globalObject = GlobalObject.getInstance()

    // 经过过滤或排序后的应用列表
    private val mAppInfoRestoreList by lazy {
        MutableStateFlow(mutableListOf<AppInfoRestore>())
    }

    private lateinit var tabLayout: TabLayout

    override fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter) {
        multiTypeAdapter.register(
            AppListAdapterRestore(
                onChipClick = { updateBadges(globalObject.appInfoRestoreMapNum) },
            )
        )
    }

    override fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any> {
        val adapterList = mutableListOf<Any>()
        when (pref.type) {
            AppListType.InstalledApp -> {
                // 安装应用
                val appList = mAppInfoRestoreList.value.filter { !it.detailBase.isSystemApp }
                adapterList.addAll(appList)
                when (pref.installedAppSelection) {
                    AppListSelection.App -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp =
                                true && it.detailRestoreList[it.restoreIndex].hasApp
                        }
                    }
                    AppListSelection.AppReverse -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp =
                                false
                        }
                    }
                    AppListSelection.All -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) {
                                it.detailRestoreList[it.restoreIndex].selectApp =
                                    true && it.detailRestoreList[it.restoreIndex].hasApp
                                it.detailRestoreList[it.restoreIndex].selectData =
                                    true && it.detailRestoreList[it.restoreIndex].hasData
                            }
                        }
                    }
                    AppListSelection.AllReverse -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) {
                                it.detailRestoreList[it.restoreIndex].selectApp = false
                                it.detailRestoreList[it.restoreIndex].selectData = false
                            }
                        }
                    }
                    else -> {}
                }
            }
            AppListType.SystemApp -> {
                // 系统应用
                val appList = mAppInfoRestoreList.value.filter { it.detailBase.isSystemApp }
                adapterList.addAll(appList)
                when (pref.systemAppSelection) {
                    AppListSelection.App -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp =
                                true && it.detailRestoreList[it.restoreIndex].hasApp
                        }
                    }
                    AppListSelection.AppReverse -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp =
                                false
                        }
                    }
                    AppListSelection.All -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) {
                                it.detailRestoreList[it.restoreIndex].selectApp =
                                    true && it.detailRestoreList[it.restoreIndex].hasApp
                                it.detailRestoreList[it.restoreIndex].selectData =
                                    true && it.detailRestoreList[it.restoreIndex].hasData
                            }

                        }
                    }
                    AppListSelection.AllReverse -> {
                        appList.forEach {
                            if (it.detailRestoreList.isNotEmpty()) {
                                it.detailRestoreList[it.restoreIndex].selectApp = false
                                it.detailRestoreList[it.restoreIndex].selectData = false
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
            if (GlobalObject.getInstance().appInfoRestoreMap.value.isEmpty()) {
                GlobalObject.getInstance().appInfoRestoreMap.emit(Command.getAppInfoRestoreMap())
            }

            mAppInfoRestoreList.emit(GlobalObject.getInstance().appInfoRestoreMap.value.values.toMutableList())

            mAppInfoRestoreList.emit(mAppInfoRestoreList.value.apply {
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
                                collator.getCollationKey((appInfo1 as AppInfoRestore).detailBase.appName)
                                    .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).detailBase.appName))
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
                                collator.getCollationKey((appInfo2 as AppInfoRestore).detailBase.appName)
                                    .compareTo(collator.getCollationKey((appInfo1 as AppInfoRestore).detailBase.appName))
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
                        sortBy { it.detailRestoreList[it.restoreIndex].sizeBytes }
                    }
                    AppListSort.DataSizeDescending -> {
                        sortByDescending { it.detailRestoreList[it.restoreIndex].sizeBytes }
                    }
                }
            })

            when (pref.filter) {
                AppListFilter.None -> {}
                AppListFilter.Selected -> {
                    mAppInfoRestoreList.emit(mAppInfoRestoreList.value.filter { if (it.detailRestoreList.isNotEmpty()) it.detailRestoreList[it.restoreIndex].selectApp || it.detailRestoreList[it.restoreIndex].selectData else false }
                        .toMutableList())
                }
                AppListFilter.NotSelected -> {
                    mAppInfoRestoreList.emit(mAppInfoRestoreList.value.filter { if (it.detailRestoreList.isNotEmpty()) !it.detailRestoreList[it.restoreIndex].selectApp && !it.detailRestoreList[it.restoreIndex].selectData else false }
                        .toMutableList())
                }
            }

            val keyWord = pref.searchKeyWord
            mAppInfoRestoreList.emit(mAppInfoRestoreList.value.filter {
                it.detailBase.appName.lowercase().contains(keyWord.lowercase()) ||
                        it.detailBase.packageName.lowercase().contains(keyWord.lowercase())
            }.toMutableList())

            // 计算已选中应用数量并应用Badges
            updateBadges(globalObject.appInfoRestoreMapNum)
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
        GsonUtil.saveAppInfoRestoreMapToFile(GlobalObject.getInstance().appInfoRestoreMap.value)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onFloatingActionButtonClick(l: () -> Unit) {
        startActivity(Intent(this, ProcessingActivity::class.java).apply {
            putExtra(ProcessingActivityTag, TypeRestoreApp)
        })
    }
}

