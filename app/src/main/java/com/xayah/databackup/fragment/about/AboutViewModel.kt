package com.xayah.databackup.fragment.about

import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.lifecycle.ViewModel

class AboutViewModel : ViewModel() {
    private fun toWebView(v: View, url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        v.context.startActivity(intent)
    }

    fun toAppGitHub(v: View) {
        toWebView(v, "https://github.com/XayahSuSuSu/Android-DataBackup")
    }

    fun toAuthorGitHub(v: View) {
        toWebView(v, "https://github.com/XayahSuSuSu")
    }


    fun toBackupScriptGitHub(v: View) {
        toWebView(v, "https://github.com/YAWAsau/backup_script")
    }
}