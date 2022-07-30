package com.xayah.databackup.activity.guide

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideViewModel : ViewModel() {
    val btnEnabled by lazy {
        MutableLiveData(true)
    }
    val btnText by lazy {
        MutableLiveData(GlobalString.nextStep)
    }
    val navigation by lazy {
        MutableLiveData<Int>()
    }
    val btnOnClick by lazy {
        MutableLiveData<(v: View) -> Unit>()
    }
    val finish by lazy {
        MutableLiveData(false)
    }
}