package com.xayah.databackup.fragment.guide

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideTwoViewModel : ViewModel() {
    val grantRootAccess = "${GlobalString.symbolDot} ${GlobalString.grantRootAccess}"
    val releasePrebuiltBinaries =
        "${GlobalString.symbolDot} ${GlobalString.releasePrebuiltBinaries}"
    val activateBashrc = "${GlobalString.symbolDot} ${GlobalString.activateBashrc}"

    var grantRootAccessCheck = ObservableField("")
    var releasePrebuiltBinariesCheck = ObservableField("")
    var activateBashrcCheck = ObservableField("")
}