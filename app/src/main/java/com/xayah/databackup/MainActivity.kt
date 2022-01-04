package com.xayah.databackup

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)
    }
}