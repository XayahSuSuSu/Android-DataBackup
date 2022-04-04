package com.xayah.databackup.fragment.home

import android.content.Context
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.io.SuFile
import com.xayah.databackup.R
import com.xayah.databackup.util.Bashrc
import com.xayah.databackup.util.Command

class HomeViewModel : ViewModel() {
    val isRoot by lazy {
        SuFile.open("/dev/console").canRead()
    }
    lateinit var abi: String
    lateinit var storageSpace: String
    var initialized = false

    fun initialize(context: Context) {
        if (!initialized) {
            abi = Command.getABI()
            storageSpace =
                if (Bashrc.getStorageSpace().first) Bashrc.getStorageSpace().second else context.getString(
                    R.string.error
                )
            initialized = true
        }
    }
}