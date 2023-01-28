package com.xayah.databackup.fragment.guide

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideEnvViewModel : ViewModel() {
    val grantRootAccessCheck: ObservableField<String> = ObservableField(GlobalString.notDetected)
    val releasePrebuiltBinariesCheck: ObservableField<String> =
        ObservableField(GlobalString.notDetected)
    val activateBashrcCheck: ObservableField<String> = ObservableField(GlobalString.notDetected)
    val storageManagementPermissionCheck: ObservableField<String> =
        ObservableField(GlobalString.notDetected)
    val packageUsageStatsPermissionCheck: ObservableField<String> =
        ObservableField(GlobalString.notDetected)
}