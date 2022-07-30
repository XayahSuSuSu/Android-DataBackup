package com.xayah.databackup.fragment.guide

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideTwoViewModel : ViewModel() {
    val grantRootAccess = "${GlobalString.symbolDot} ${GlobalString.grantRootAccess}"
    val releasePrebuiltBinaries =
        "${GlobalString.symbolDot} ${GlobalString.releasePrebuiltBinaries}"
    val activateBashrc = "${GlobalString.symbolDot} ${GlobalString.activateBashrc}"

    val grantRootAccessCheck by lazy {
        MutableLiveData("")
    }
    val releasePrebuiltBinariesCheck by lazy {
        MutableLiveData("")
    }
    val activateBashrcCheck by lazy {
        MutableLiveData("")
    }
}