package com.xayah.databackup.fragment.guide

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideEnvViewModel : ViewModel() {
    val grantRootAccess = "${GlobalString.symbolDot} ${GlobalString.grantRootAccess}"
    val releasePrebuiltBinaries =
        "${GlobalString.symbolDot} ${GlobalString.releasePrebuiltBinaries}"
    val activateBashrc = "${GlobalString.symbolDot} ${GlobalString.activateBashrc}"
    val packageUsageStatsPermission =
        "${GlobalString.symbolDot} ${GlobalString.checkPackageUsageStatsPermission}"

    val grantRootAccessCheck: ObservableField<String> = ObservableField(GlobalString.notSelected)
    val releasePrebuiltBinariesCheck: ObservableField<String> =
        ObservableField(GlobalString.notSelected)
    val activateBashrcCheck: ObservableField<String> = ObservableField(GlobalString.notSelected)
    val packageUsageStatsPermissionCheck: ObservableField<String> =
        ObservableField(GlobalString.notSelected)
}