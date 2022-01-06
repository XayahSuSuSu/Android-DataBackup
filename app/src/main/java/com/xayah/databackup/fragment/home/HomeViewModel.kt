package com.xayah.databackup.fragment.home

import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.util.ShellUtil

class HomeViewModel : ViewModel() {
    val isRoot = Shell.getShell().isRoot
    lateinit var scriptVersion: String
    val spaceUsed = ShellUtil.getStorageSpace()[2].replace("G", "")
    val spaceTotal = ShellUtil.getStorageSpace()[1].replace("G", "")
}