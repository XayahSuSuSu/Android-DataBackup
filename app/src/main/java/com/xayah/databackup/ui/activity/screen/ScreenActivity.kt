package com.xayah.databackup.ui.activity.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xayah.databackup.ui.activity.guide.GuideActivity
import com.xayah.databackup.ui.activity.main.MainActivity
import com.xayah.databackup.util.command.Command
import com.xayah.databackup.util.readInitializedVersionName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
class ScreenActivity : AppCompatActivity() {
    private val latch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        CoroutineScope(Dispatchers.IO).launch {
            if (readInitializedVersionName().isEmpty()) {
                // First install
                startActivity(Intent(this@ScreenActivity, GuideActivity::class.java))
            } else if (Command.checkRoot().not() || Command.checkBin().not()) {
                startActivity(Intent(this@ScreenActivity, GuideActivity::class.java))
            } else {
                startActivity(Intent(this@ScreenActivity, MainActivity::class.java))
            }
            latch.countDown()
            finish()
        }

        try {
            latch.await()
        } catch (_: Exception) {

        }
    }
}