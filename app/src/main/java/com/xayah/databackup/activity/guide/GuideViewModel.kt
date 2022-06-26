package com.xayah.databackup.activity.guide

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideViewModel : ViewModel() {
    var btnText = ObservableField(GlobalString.nextStep)
}