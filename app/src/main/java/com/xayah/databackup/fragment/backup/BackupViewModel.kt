package com.xayah.databackup.fragment.backup

import androidx.lifecycle.ViewModel
import com.drakeet.multitype.MultiTypeAdapter
import com.xayah.databackup.data.AppEntity
import com.xayah.databackup.databinding.FragmentBackupBinding

class BackupViewModel : ViewModel() {
    var binding: FragmentBackupBinding? = null

    var isProcessing: Boolean = false

    var appList: MutableList<AppEntity> = mutableListOf()
    lateinit var mAdapter: MultiTypeAdapter

    var time: Long = 0
    var index = 0
    var total = 0

    var success = 0
    var failed = 0
}