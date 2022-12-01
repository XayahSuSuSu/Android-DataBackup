package com.xayah.databackup.activity.list

import androidx.lifecycle.MutableLiveData
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.adapter.AppListAdapterBackup
import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.AppListSort
import com.xayah.databackup.data.AppListType
import com.xayah.databackup.util.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class AppListRestoreActivity : AppListBaseActivity() {
    companion object {
        const val TAG = "AppListRestoreActivity"
    }

    private val _mAppInfoList by lazy {
        MutableLiveData(mutableListOf<AppInfoRestore>())
    }
    var mAppInfoList
        get() = _mAppInfoList.value!!
        set(value) = _mAppInfoList.postValue(value)

    override fun onAdapterRegister(multiTypeAdapter: MultiTypeAdapter) {
        multiTypeAdapter.register(AppListAdapterBackup())
    }

    override fun onAdapterListAdd(pref: AppListPreferences): MutableList<Any> {
        val adapterList = mutableListOf<Any>()
        when (pref.type) {
            AppListType.InstalledApp -> {
                // 安装应用
                adapterList.addAll(mAppInfoList.filter { !it.infoBase.isSystemApp })
            }
            AppListType.SystemApp -> {
                // 系统应用
                adapterList.addAll(mAppInfoList.filter { it.infoBase.isSystemApp })
            }
        }
        return adapterList
    }

    override suspend fun loadList(pref: AppListPreferences) {
        withContext(Dispatchers.IO) {
            mAppInfoList = Command.getCachedAppInfoRestoreActualList().apply {
                when (pref.sort) {
                    AppListSort.Alphabet -> {
                        sortWith { appInfo1, appInfo2 ->
                            val collator = Collator.getInstance(Locale.CHINA)
                            collator.getCollationKey((appInfo1 as AppInfoRestore).infoBase.appName)
                                .compareTo(collator.getCollationKey((appInfo2 as AppInfoRestore).infoBase.appName))
                        }
                    }
                }
            }
        }
    }
}
