package com.xayah.databackup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.databinding.ActivityMainBinding
import com.xayah.databackup.util.ShellUtil
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean


class MainActivity : AppCompatActivity() {
    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG;
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }

    lateinit var binding: ActivityMainBinding
    lateinit var mShell: com.xayah.databackup.util.Shell
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)

        binding()
        init()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.isRoot = Shell.getShell().isRoot
        binding.largeActionCardSelectApps.setOnClickListener {
            val intent = Intent(this, SelectActivity::class.java)
            startActivityForResult(intent, 2)
        }
        binding.largeActionCardBackup.setOnClickListener {
            mShell.onBackup()
        }
    }

    private fun init() {
        mShell = com.xayah.databackup.util.Shell(this)
        mShell.extractAssets()
        binding.backupNum = ShellUtil.countSelected(mShell.APP_LIST_FILE_PATH)
        Shell.getShell()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 2) {
            binding.backupNum = ShellUtil.countSelected(mShell.APP_LIST_FILE_PATH)
        }
    }
}