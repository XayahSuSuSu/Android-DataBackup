package com.xayah.databackup.activity.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xayah.databackup.App
import com.xayah.databackup.compose.ui.activity.guide.GuideActivity
import com.xayah.databackup.compose.ui.activity.main.MainActivity
import com.xayah.databackup.util.checkPackageUsageStatsPermission
import com.xayah.databackup.util.readInitializedVersionName
import com.xayah.databackup.util.readIsSupportUsageAccess

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class ScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        if ((readInitializedVersionName() == App.versionName) &&
            (checkPackageUsageStatsPermission() || readIsSupportUsageAccess().not())
        ) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, GuideActivity::class.java))
        }
        finish()
    }
}