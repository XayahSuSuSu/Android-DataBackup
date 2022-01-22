package com.xayah.databackup.fragment.home

import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.util.ShellUtil

class HomeViewModel : ViewModel() {
    val isRoot = Shell.getShell().isRoot
    lateinit var scriptVersion: String
    lateinit var storageSpace:String
}