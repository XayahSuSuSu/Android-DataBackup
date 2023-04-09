package com.xayah.databackup.ui.activity.settings

import android.content.pm.PackageInfo
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.xayah.databackup.ui.activity.settings.components.SingleChoiceTextClickableItem
import com.xayah.databackup.ui.activity.settings.components.SwitchItem
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel : ViewModel() {
    val isInitialized by lazy { MutableTransitionState(false) }

    // 应用相关设置项
    val newestVersion by lazy { mutableStateOf("") }
    val newestVersionLink by lazy { mutableStateOf("") }
    val suspendedPackages by lazy {
        MutableStateFlow(SnapshotStateList<PackageInfo>())
    }

    // 备份相关设置项
    val backupSwitchItems by lazy {
        MutableStateFlow(SnapshotStateList<SwitchItem>())
    }

    // 恢复相关设置项
    val supportAutoFixMultiUserContext by lazy { mutableStateOf(false) }
    val restoreSwitchItems by lazy {
        MutableStateFlow(SnapshotStateList<SwitchItem>())
    }

    // 用户相关设置项
    val userSingleChoiceTextClickableItemsItems by lazy {
        MutableStateFlow(SnapshotStateList<SingleChoiceTextClickableItem>())
    }
}
