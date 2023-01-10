package com.xayah.databackup.fragment.guide

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.appReleaseList
import kotlinx.coroutines.launch

class GuideUpdateViewModel : ViewModel() {
    val subtitle by lazy {
        MutableLiveData(GlobalString.fetching)
    }
    val content by lazy {
        MutableLiveData(GlobalString.fetching)
    }

    fun initialize() {
        viewModelScope.launch {
            subtitle.postValue("${GlobalString.currentVersion}: ${App.versionName}")
            App.server.releases({ releaseList ->
                val mReleaseList = releaseList.appReleaseList()
                if (mReleaseList.isEmpty()) {
                    content.postValue(GlobalString.fetchFailed)
                } else {
                    var info = ""
                    for (i in mReleaseList) {
                        info += "${i.name}\n"
                        info += "${i.body.replace("*", GlobalString.symbolDot)}\n\n"
                    }
                    content.postValue(info)
                }
            }, {
                content.postValue(GlobalString.fetchFailed)
            })
        }
    }
}