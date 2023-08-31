package com.xayah.databackup.ui.activity.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.xayah.databackup.ui.activity.guide.GuideActivity
import com.xayah.databackup.ui.activity.main.MainActivity
import com.xayah.databackup.util.command.EnvUtil.getCurrentAppVersionName
import com.xayah.databackup.util.readAppVersionName
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    // Workaround for HarmonyOS
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()

        if (getCurrentAppVersionName() > readAppVersionName()) {
            // There is an update
            startActivity(Intent(this, GuideActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
