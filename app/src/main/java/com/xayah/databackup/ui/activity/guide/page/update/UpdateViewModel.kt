package com.xayah.databackup.ui.activity.guide.page.update

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.util.Release
import com.xayah.databackup.util.ServerUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(private val serverUtil: ServerUtil) : ViewModel() {
    var releases: MutableState<List<Release>> = mutableStateOf(listOf())

    suspend fun getReleases(onSucceed: (releases: List<Release>) -> Unit, onFailed: () -> Unit) {
        serverUtil.getReleases(
            onSucceed = {
                releases.value = it
                onSucceed(it)
            },
            onFailed = onFailed
        )
    }
}
