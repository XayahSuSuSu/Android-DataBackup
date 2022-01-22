package com.xayah.databackup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xayah.databackup.databinding.ActivityCrashBinding
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean


class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityCrashBinding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)

        binding.textView.text = String.format(
            getString(R.string.crash_content),
            this.filesDir.path.replace("/files", "") + "/Crash/"
        )
    }
}