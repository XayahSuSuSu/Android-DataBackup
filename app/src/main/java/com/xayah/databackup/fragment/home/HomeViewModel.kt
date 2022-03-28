package com.xayah.databackup.fragment.home

import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.io.SuFile

class HomeViewModel : ViewModel() {
    val isRoot by lazy {
        SuFile.open("/dev/console").canRead()
    }
    lateinit var scriptVersion: String
    lateinit var storageSpace: String
}