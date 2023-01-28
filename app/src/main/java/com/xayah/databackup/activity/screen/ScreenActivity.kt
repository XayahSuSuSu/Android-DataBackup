package com.xayah.databackup.activity.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xayah.databackup.App
import com.xayah.databackup.activity.guide.GuideActivity
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.readIsAllowRoot

class ScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        if (App.globalContext.readIsAllowRoot()) {
            GlobalObject.getInstance().suFile.initialize(App.globalContext) {
                startActivity(Intent(this, GuideActivity::class.java))
                finish()
            }
        } else {
            startActivity(Intent(this, GuideActivity::class.java))
            finish()
        }
    }
}