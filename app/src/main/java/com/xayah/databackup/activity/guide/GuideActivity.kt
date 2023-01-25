package com.xayah.databackup.activity.guide

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.activity.main.MainActivity
import com.xayah.databackup.databinding.ActivityGuideBinding
import com.xayah.databackup.util.GlobalString
import com.xayah.databackup.util.checkPackageUsageStatsPermission
import com.xayah.databackup.util.readInitializedVersionName
import com.xayah.databackup.util.readIsSupportUsageAccess

class GuideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuideBinding
    private lateinit var viewModel: GuideViewModel
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        installSplashScreen()

        binding = ActivityGuideBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[GuideViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        judgePage()

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_guide) as NavHostFragment
        navController = navHostFragment.navController

        viewModel.btnPrevOnClick.observe(this) {
            binding.materialButtonPrev.setOnClickListener(it)
        }
        viewModel.btnNextOnClick.observe(this) {
            binding.materialButtonNext.setOnClickListener(it)
        }
        viewModel.navigation.observe(this) {
            navController.navigate(it)
        }
        viewModel.finishAndEnter.observe(this) {
            if (it) toMain()
        }
        viewModel.finish.observe(this) {
            if (it) finish()
        }
    }

    private fun toMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * 判断是否是第一次打开
     */
    private fun judgePage() {
        if (App.globalContext.readInitializedVersionName().isNotEmpty()) {
            if (App.globalContext.readInitializedVersionName() != App.versionName) {
                // 版本更新
                viewModel.navigation.postValue(R.id.action_guideIntroductionFragment_to_guideUpdateFragment)
                viewModel.btnNextText.postValue(GlobalString.finish)
            } else if (App.globalContext.checkPackageUsageStatsPermission().not()
                && App.globalContext.readIsSupportUsageAccess()
            ) {
                // 权限不够
                viewModel.navigation.postValue(R.id.action_guideIntroductionFragment_to_guideEnvFragment)
            } else {
                toMain()
            }
        }
    }
}