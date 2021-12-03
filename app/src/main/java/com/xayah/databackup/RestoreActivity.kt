package com.xayah.databackup

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.xayah.databackup.adapter.BackupListAdapter
import com.xayah.databackup.databinding.ActivityRestoreBinding
import com.xayah.databackup.model.BackupInfo
import com.xayah.databackup.util.*


class RestoreActivity : AppCompatActivity() {
    lateinit var mContext: Context
    lateinit var binding: ActivityRestoreBinding
    lateinit var adapter: BackupListAdapter
    lateinit var mShell: Shell

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        setTitle(R.string.restore)
        mContext = this

        binding()
        init()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_restore)
    }

    private fun init() {
        mShell = Shell(this)
        adapter = BackupListAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBackupList.layoutManager = layoutManager
        val files = ShellUtil.countFiles(mShell.BACKUP_PATH)
        for ((index, i) in files.withIndex()) {
            val backupInfo = BackupInfo("备份存档${index + 1}", DataUtil.getFormatDate(i.split("_")[1].toLong()))
            adapter.addBackup(backupInfo)
        }
        binding.recyclerViewBackupList.adapter = adapter
        (binding.recyclerViewBackupList.itemAnimator as DefaultItemAnimator).supportsChangeAnimations =
            false
    }
}