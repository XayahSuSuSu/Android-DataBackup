package com.xayah.databackup.fragment.guide

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuideUpdateViewModel : ViewModel() {
    val subtitle by lazy {
        MutableLiveData(GlobalString.fetching)
    }
    val content by lazy {
        MutableLiveData(GlobalString.fetching)
    }

    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            Server.releases({ releaseList ->
                val mReleaseList = releaseList.filter { !it.name.contains("Check") }
                if (mReleaseList.isEmpty()) {
                    subtitle.postValue(GlobalString.fetchFailed)
                    content.postValue(GlobalString.fetchFailed)
                } else {
                    subtitle.postValue(mReleaseList[0].name)
                    content.postValue(mReleaseList[0].body.replace("*", GlobalString.symbolDot))
                }
            }, {
                subtitle.postValue(GlobalString.fetchFailed)
                content.postValue(GlobalString.fetchFailed)
            })
        }
    }
}