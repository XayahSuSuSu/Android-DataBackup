package com.xayah.databackup.activity.guide

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString

class GuideViewModel : ViewModel() {
    val btnEnabled by lazy {
        MutableLiveData(true)
    }
    val btnPrevText by lazy {
        MutableLiveData(GlobalString.disagree)
    }
    val btnNextText by lazy {
        MutableLiveData(GlobalString.agree)
    }
    val navigation by lazy {
        MutableLiveData<Int>()
    }
    val btnPrevOnClick by lazy {
        MutableLiveData<(v: View) -> Unit> { finish.postValue(true) }
    }
    val btnNextOnClick by lazy {
        MutableLiveData<(v: View) -> Unit>()
    }
    val finishAndEnter by lazy {
        MutableLiveData(false)
    }
    val finish by lazy {
        MutableLiveData(false)
    }
}