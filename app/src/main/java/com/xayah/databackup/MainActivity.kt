package com.xayah.databackup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
                    .setInitializers(ScriptInitializer::class.java)
            )
        }
    }

    class ScriptInitializer : Shell.Initializer() {
        override fun onInit(context: Context, shell: Shell): Boolean {
            shell.newJob()
                .add("export APP_ENV=1")
                .exec()
            return true
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

    override fun onDestroy() {
        super.onDestroy()
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
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_query_tips))
                .setMessage(getString(R.string.dialog_query_backup))
                .setNegativeButton(getString(R.string.dialog_query_negative)) { _, _ -> }
                .setPositiveButton(getString(R.string.dialog_query_positive)) { _, _ -> mShell.onBackup() }
                .show()
        }

        binding.largeActionLabelSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.largeActionLabelCredits.setOnClickListener {
            val dialogBinding = DialogCreditsBinding.inflate(this.layoutInflater).apply {
                this.linearLayoutScriptAuthor.setOnClickListener {
                    val uri = Uri.parse("http://www.coolapk.com/u/2277637")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                this.linearLayoutScriptSimplify.setOnClickListener {
                    val uri = Uri.parse("https://github.com/Petit-Abba")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                this.linearLayoutClash.setOnClickListener {
                    val uri = Uri.parse("https://github.com/Kr328/ClashForAndroid")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                this.linearLayoutMagisk.setOnClickListener {
                    val uri = Uri.parse("https://github.com/topjohnwu/Magisk")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
            }

            AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .show()
        }

        binding.largeActionLabelAbout.setOnClickListener {
            val dialogBinding = DialogAboutBinding.inflate(this.layoutInflater).apply {
                this.versionName = packageManager.getPackageInfo(packageName, 0).versionName
                this.linearLayoutApp.setOnClickListener {
                    val uri = Uri.parse("https://github.com/XayahSuSuSu/Android-DataBackup")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
                this.linearLayoutAuthor.setOnClickListener {
                    val uri = Uri.parse("http://www.coolapk.com/u/1394294")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }
            }

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