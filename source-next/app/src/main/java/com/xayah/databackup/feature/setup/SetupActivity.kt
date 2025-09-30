package com.xayah.databackup.feature.setup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.NotificationHelper
import kotlinx.serialization.Serializable

const val NoPermKey = "NoPerm"

@Serializable
data object Welcome

@Serializable
data class Permissions(
    val enableBackBtn: Boolean
)

class SetupActivity : ComponentActivity() {
    private lateinit var mPermissionsViewModel: PermissionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DataBackupTheme {
                val navController = rememberNavController()
                mPermissionsViewModel = viewModel()
                val noPerm = intent.getBooleanExtra(NoPermKey, false)

                Surface {
                    NavHost(
                        navController = navController,
                        startDestination = if (noPerm) Permissions(false) else Welcome,
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow))
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow))
                        },
                    ) {
                        composable<Welcome> { WelcomeScreen(navController) }
                        composable<Permissions> { backStackEntry ->
                            val permissions = backStackEntry.toRoute<Permissions>()
                            PermissionsScreen(navController, mPermissionsViewModel, permissions)
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        when (requestCode) {
            NotificationHelper.REQUEST_CODE -> {
                mPermissionsViewModel.withLock {
                    mPermissionsViewModel.checkNotification(this)
                }
            }
        }
    }
}
