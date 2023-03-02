package com.xayah.databackup.ui.activity.blacklist

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.BlackListItem
import com.xayah.databackup.util.Command
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.readBlackListMapPath
import com.xayah.databackup.util.saveBlackListMapPath
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class BlackListViewModel : ViewModel() {
    lateinit var explorer: MaterialYouFileExplorer

    val blackList by lazy {
        MutableStateFlow(SnapshotStateList<BlackListItem>())
    }

    fun initializeExplorer(activity: ComponentActivity) {
        explorer = MaterialYouFileExplorer().apply {
            initialize(activity)
        }
    }

    suspend fun initializeList(context: Context) {
        blackList.value.clear()
        blackList.value.addAll(Command.readBlackListMap(context.readBlackListMapPath()).values)
    }

    suspend fun removeItem(context: Context, item: BlackListItem) {
        Command.removeBlackList(context.readBlackListMapPath(), item.packageName)
        blackList.value.remove(item)
        GlobalObject.getInstance().appInfoBackupMap.value.clear()
    }

    fun importConfig(context: Context) {
        explorer.apply {
            isFile = true
            toExplorer(context) { path, _ ->
                context.saveBlackListMapPath(path)
                viewModelScope.launch {
                    initializeList(context)
                }
            }
        }
    }
}
