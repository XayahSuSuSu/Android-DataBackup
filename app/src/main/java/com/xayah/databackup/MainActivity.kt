package com.xayah.databackup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.databinding.ActivityMainBinding
import com.xayah.databackup.databinding.DialogAboutBinding
import com.xayah.databackup.databinding.DialogCreditsBinding
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

        binding.largeActionCardRoot.setOnClickListener {
            binding.isRoot = Shell.getShell().isRoot
        }

        binding.largeActionCardSelectApps.setOnClickListener {
            val intent = Intent(this, SelectActivity::class.java)
            startActivityForResult(intent, 2)
        }

        binding.largeActionCardRestore.setOnClickListener {
            val intent = Intent(this, RestoreActivity::class.java)
            startActivity(intent)
        }

        binding.largeActionCardBackup.setOnClickListener {
            mShell.onBackup()
        }

        binding.largeActionLabelSettings.setOnClickListener {
            Snackbar.make(binding.constraintLayoutMain, getString(R.string.wip), Snackbar.LENGTH_SHORT)
                .show()
        }

        binding.largeActionLabelCredits.setOnClickListener {
            val dialogBinding = DialogCreditsBinding.inflate(this.layoutInflater).apply {
            }

            AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .show()
        }

        binding.largeActionLabelAbout.setOnClickListener {
            val dialogBinding = DialogAboutBinding.inflate(this.layoutInflater).apply {
                this.versionName = packageManager.getPackageInfo(packageName, 0).versionName
            }
//            val uri = Uri.parse("https://github.com/XayahSuSuSu/Android-DataBackup")
//            val intent = Intent(Intent.ACTION_VIEW, uri)
//            startActivity(intent)

            AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .show()
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