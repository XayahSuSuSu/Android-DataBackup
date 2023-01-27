package com.xayah.databackup.activity.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xayah.databackup.activity.guide.GuideActivity
import com.xayah.databackup.util.GlobalObject

class ScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        GlobalObject.getInstance().suFile.initialize(this){
            startActivity(Intent(this, GuideActivity::class.java))
            finish()
        }
    }
}