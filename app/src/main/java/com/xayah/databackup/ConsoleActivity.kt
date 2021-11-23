package com.xayah.databackup

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.xayah.databackup.databinding.ActivityConsoleBinding
import com.xayah.databackup.util.ShellUtil
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ConsoleActivity : AppCompatActivity() {
    private lateinit var mShell: ShellUtil
    private lateinit var binding: ActivityConsoleBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_console)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        binding()
        init()
    }

    private fun binding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_console)
        binding.extendedFloatingActionButton.setOnClickListener {
            finish()
        }
    }

    private fun init() {
        mShell = ShellUtil(this)
        when (intent.getStringExtra("type")) {
            "generateAppList" -> {
                setTitle(R.string.title_generateAppList)
                mShell.generateAppList()
                generateAppList()
            }
        }
    }

    private fun generateAppList() {
        GlobalScope.launch() {
            try {
                while (!mShell.isSuccess) {
                    if (mShell.console.isNotEmpty()) {
                        binding.logs =
                            mShell.console.joinToString(separator = "\n")
                                .replace("\u001B[0m", "")
                                .replace("  -", " -")
                                .replace("(.*?)m -".toRegex(), " -")
                        runOnUiThread {
                            binding.nestedScrollViewConsole.fullScroll(View.FOCUS_DOWN);
                        }
                    }
                }
                binding.isFinished = true
                runOnUiThread {
                    setTitle(R.string.title_finished)
                }
                val intent = Intent()
                intent.putExtra("appNum", mShell.countLine(mShell.appListFilePath) - 2)
                setResult(1, intent)

            } catch (e: ConcurrentModificationException) {
                e.printStackTrace()
                generateAppList()
            }
        }
    }

}