package com.xayah.databackup.viewModel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.R
import com.xayah.databackup.RestoreActivity
import com.xayah.databackup.SelectActivity
import com.xayah.databackup.SettingsActivity
import com.xayah.databackup.databinding.DialogAboutBinding
import com.xayah.databackup.databinding.DialogCreditsBinding

class MainViewModel : ViewModel() {
    val isRoot: Boolean = Shell.getShell().isRoot
    var backupNum = 0

    fun onBackup(v: View) {
        val mContext = v.context
        MaterialAlertDialogBuilder(mContext)
            .setTitle(mContext.getString(R.string.dialog_query_tips))
            .setMessage(mContext.getString(R.string.dialog_query_backup))
            .setNegativeButton(mContext.getString(R.string.dialog_query_negative)) { _, _ -> }
            .setPositiveButton(mContext.getString(R.string.dialog_query_positive)) { _, _ ->
                val mShell = com.xayah.databackup.util.Shell(mContext as Activity)
                mShell.onBackup()
            }
            .show()
    }

    fun toSelectActivity(v: View) {
        val intent = Intent(v.context, SelectActivity::class.java)
        v.context.startActivity(intent)
    }

    fun toRestoreActivity(v: View) {
        val intent = Intent(v.context, RestoreActivity::class.java)
        v.context.startActivity(intent)
    }

    fun toSettingsActivity(v: View) {
        val intent = Intent(v.context, SettingsActivity::class.java)
        v.context.startActivity(intent)
    }

    fun toCreditsDialog(v: View) {
        val dialogBinding =
            DialogCreditsBinding.inflate((v.context as Activity).layoutInflater).apply {
                this.linearLayoutScriptAuthor.setOnClickListener {
                    val uri = Uri.parse("http://www.coolapk.com/u/2277637")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    v.context.startActivity(intent)
                }
                this.linearLayoutScriptSimplify.setOnClickListener {
                    val uri = Uri.parse("https://github.com/Petit-Abba")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    v.context.startActivity(intent)
                }
                this.linearLayoutClash.setOnClickListener {
                    val uri = Uri.parse("https://github.com/Kr328/ClashForAndroid")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    v.context.startActivity(intent)
                }
                this.linearLayoutMagisk.setOnClickListener {
                    val uri = Uri.parse("https://github.com/topjohnwu/Magisk")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    v.context.startActivity(intent)
                }
            }

        AlertDialog.Builder(v.context)
            .setView(dialogBinding.root)
            .show()
    }

    fun toAboutDialog(v: View) {
        val dialogBinding =
            DialogAboutBinding.inflate((v.context as Activity).layoutInflater).apply {
                this.versionName =
                    v.context.packageManager.getPackageInfo(v.context.packageName, 0).versionName
                this.linearLayoutApp.setOnClickListener {
                    val uri = Uri.parse("https://github.com/XayahSuSuSu/Android-DataBackup")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    v.context.startActivity(intent)
                }
                this.linearLayoutAuthor.setOnClickListener {
                    val uri = Uri.parse("http://www.coolapk.com/u/1394294")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    v.context.startActivity(intent)
                }
            }

        AlertDialog.Builder(v.context)
            .setView(dialogBinding.root)
            .show()
    }
}