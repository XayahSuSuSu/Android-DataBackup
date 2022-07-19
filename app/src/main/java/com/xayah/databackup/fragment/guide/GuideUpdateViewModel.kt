package com.xayah.databackup.fragment.guide

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuideUpdateViewModel : ViewModel() {
    var subtitle = ObservableField(GlobalString.fetching)
    var content = ObservableField(GlobalString.fetching)

    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            Server.releases({ releaseList ->
                val mReleaseList = releaseList.filter { !it.name.contains("Check") }
                if (mReleaseList.isEmpty()) {
                    subtitle.set(GlobalString.fetchFailed)
                    content.set(GlobalString.fetchFailed)
                } else {
                    subtitle.set(mReleaseList[0].name)
                    content.set(mReleaseList[0].body.replace("*", GlobalString.symbolDot))
                }
            }, {
                subtitle.set(GlobalString.fetchFailed)
                content.set(GlobalString.fetchFailed)
            })
        }
    }
}