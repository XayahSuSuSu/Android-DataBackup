package com.xayah.databackup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean

class RestoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
        setTitle(R.string.restore)
    }
}