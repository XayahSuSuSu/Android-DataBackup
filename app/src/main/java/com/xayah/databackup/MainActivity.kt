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
    lateinit var binding: ActivityMainBinding
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
            startActivity(intent)
        }
    }

    private fun init() {
        val mShell = ShellUtil(this)
        mShell.extractAssets()
    }
}