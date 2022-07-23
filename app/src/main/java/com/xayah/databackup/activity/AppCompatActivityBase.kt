package com.xayah.databackup.activity

import androidx.appcompat.app.AppCompatActivity
import com.xayah.databackup.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class AppCompatActivityBase : AppCompatActivity() {
    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch {
            App.saveGlobalList()
        }
    }
}