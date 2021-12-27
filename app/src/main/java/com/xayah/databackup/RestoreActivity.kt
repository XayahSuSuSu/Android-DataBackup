package com.xayah.databackup

import android.content.Context
import android.content.Intent
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
        mContext = this

        binding()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_restore)
        binding.extendedFloatingActionButton.setOnClickListener {
            val intent = Intent(mContext, ConsoleActivity::class.java)
            intent.putExtra("type", "restore")
            intent.putExtra("name", "Backup_zstd")
            startActivity(intent)
        }
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.title = getString(R.string.restore)
    }

    private fun init() {
        mShell = Shell(this)
        adapter = BackupListAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBackupList.layoutManager = layoutManager
        val backupInfo = BackupInfo("备份存档", "时间显示暂不可用")
        adapter.addBackup(backupInfo)
        binding.recyclerViewBackupList.adapter = adapter
        (binding.recyclerViewBackupList.itemAnimator as DefaultItemAnimator).supportsChangeAnimations =
            false
    }
}