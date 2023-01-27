package com.xayah.databackup.util

class GlobalObject {
    object Instance {
        val instance = GlobalObject()
    }

    companion object {
        fun getInstance() = Instance.instance
    }

    val suFile = SuFile.getInstance()
}