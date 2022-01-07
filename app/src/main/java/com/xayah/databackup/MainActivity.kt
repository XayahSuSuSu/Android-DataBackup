package com.xayah.databackup

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.databinding.ActivityMainBinding
import com.xayah.databackup.util.WindowUtil
import com.xayah.databackup.util.resolveThemedBoolean
import com.xayah.databackup.viewModel.MainViewModel


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

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowUtil.setWindowMode(!resolveThemedBoolean(android.R.attr.windowLightStatusBar), window)

        val model: MainViewModel by viewModels()
        binding.viewModel = model

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.setOnNavigationItemReselectedListener {  }
    }
}