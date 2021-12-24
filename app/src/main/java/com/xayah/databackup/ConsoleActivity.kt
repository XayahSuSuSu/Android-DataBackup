package com.xayah.databackup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.xayah.databackup.databinding.ActivityConsoleBinding
import com.xayah.databackup.util.Shell
import com.xayah.databackup.util.ShellUtil
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean

class ConsoleActivity : AppCompatActivity() {
    private lateinit var mShell: Shell
    private lateinit var binding: ActivityConsoleBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        binding()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        mShell.close()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_console)
        binding.extendedFloatingActionButton.setOnClickListener {
            finish()
        }
    }

    private fun init() {
        mShell = Shell(this)
        when (intent.getStringExtra("type")) {
            "generateAppList" -> {
                binding.topAppBar.title = getString(R.string.title_generateAppList)
                binding.logs = ""
                mShell.generateAppList({
                    binding.logs += it.replace("\u001B[0m", "").replace("  -", " -")
                        .replace("(.*?)m -".toRegex(), " -") + "\n"
                    runOnUiThread {
                        binding.nestedScrollViewConsole.fullScroll(View.FOCUS_DOWN);
                    }
                }, {
                    if (it == true) {
                        binding.isFinished = true
                        runOnUiThread {
                            binding.topAppBar.title = getString(R.string.title_finished)
                        }
                        val intent = Intent()
                        intent.putExtra(
                            "appNum",
                            ShellUtil.countLine(mShell.APP_LIST_FILE_PATH) - 2
                        )
                        setResult(1, intent)
                    }
                })
            }
            "backup"->{
                binding.topAppBar.title  = getString(R.string.title_backup)
                binding.logs = ""
                mShell.backup({
                    binding.logs += it.replace("\u001B[0m", "").replace("  -", " -")
                        .replace("(.*?)m -".toRegex(), " -") + "\n"
                    runOnUiThread {
                        binding.nestedScrollViewConsole.fullScroll(View.FOCUS_DOWN);
                    }
                }, {
                    if (it == true) {
                        binding.isFinished = true
                        runOnUiThread {
                            binding.topAppBar.title = getString(R.string.title_finished)
                        }
//                        ShellUtil.mv(
//                            "${mShell.SCRIPT_PATH}/Backup_zstd",
//                            "${mShell.BACKUP_PATH}/Backup_zstd"
//                        )
//                        ShellUtil.rename(
//                            "Backup_zstd",
//                            "Backup_${System.currentTimeMillis()}",
//                            mShell.BACKUP_PATH
//                        )
                    }
                })
            }
            "restore" -> {
                binding.topAppBar.title  = getString(R.string.restore)
                binding.logs = ""
                val name = intent.getStringExtra("name")
                if (name != null) {
                    mShell.restore(name, {
                        binding.logs += it.replace("\u001B[0m", "").replace("  -", " -")
                            .replace("(.*?)m -".toRegex(), " -") + "\n"
                        runOnUiThread {
                            binding.nestedScrollViewConsole.fullScroll(View.FOCUS_DOWN);
                        }
                    }, {
                        if (it == true) {
                            binding.isFinished = true
                            runOnUiThread {
                                binding.topAppBar.title = getString(R.string.title_finished)
                            }
                        }
                    })
                }
            }
        }
    }


}