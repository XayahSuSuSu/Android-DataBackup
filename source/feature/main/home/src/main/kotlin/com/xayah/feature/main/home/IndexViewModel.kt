package com.xayah.feature.main.home

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.datastore.readLastBackupTime
import com.xayah.core.ui.route.MainRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class IndexUiState(
    val lastBackupTime: Flow<String>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data class ToPagePackages(val navController: NavHostController) : IndexUiIntent()
    data class ToPageTaskList(val navController: NavHostController) : IndexUiIntent()
    data class ToPageDirectory(val navController: NavHostController) : IndexUiIntent()
    data class ToPageTree(val navController: NavHostController) : IndexUiIntent()
    data class ToPageLog(val navController: NavHostController) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        lastBackupTime = context.readLastBackupTime().distinctUntilChanged(),
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.ToPagePackages -> {
                withMainContext {
                    intent.navController.navigate(MainRoutes.Packages.route)
                }
            }

            is IndexUiIntent.ToPageTaskList -> {
                withMainContext {
                    intent.navController.navigate(MainRoutes.TaskList.route)
                }
            }

            is IndexUiIntent.ToPageDirectory -> {
                withMainContext {
                    intent.navController.navigate(MainRoutes.Directory.route)
                }
            }

            is IndexUiIntent.ToPageTree -> {
                withMainContext {
                    intent.navController.navigate(MainRoutes.Tree.route)
                }
            }

            is IndexUiIntent.ToPageLog -> {
                withMainContext {
                    intent.navController.navigate(MainRoutes.Log.route)
                }
            }
        }
    }
}
