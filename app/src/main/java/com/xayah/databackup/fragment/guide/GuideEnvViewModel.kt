package com.xayah.databackup.fragment.guide

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideEnvViewModel : ViewModel() {
    val grantRootAccess = "${GlobalString.symbolDot} ${GlobalString.grantRootAccess}"
    val releasePrebuiltBinaries =
        "${GlobalString.symbolDot} ${GlobalString.releasePrebuiltBinaries}"
    val activateBashrc = "${GlobalString.symbolDot} ${GlobalString.activateBashrc}"
    val packageUsageStatsPermission =
        "${GlobalString.symbolDot} ${GlobalString.checkPackageUsageStatsPermission}"

    val grantRootAccessCheck by lazy {
        MutableLiveData("")
    }
    val releasePrebuiltBinariesCheck by lazy {
        MutableLiveData("")
    }
    val activateBashrcCheck by lazy {
        MutableLiveData("")
    }
    val packageUsageStatsPermissionCheck by lazy {
        MutableLiveData("")
    }
}