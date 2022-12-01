package com.xayah.databackup.activity.list

import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter

class AppListBaseViewModel : ViewModel() {
    val mAdapter = MultiTypeAdapter()
    var pref = AppListPreferences()
}
