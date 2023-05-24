package com.xayah.databackup.ui.activity.main.page.guide

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.ui.activity.main.router.GuideRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class GuideUiState(
    val title: String,
    val icon: ImageVector,
    val fabIcon: ImageVector,
    val onFabClick: (scaffoldNavController: NavHostController, navController: NavHostController) -> Unit
) {
    class Intro(title: String = "") : GuideUiState(
        title = title,
        icon = Icons.Rounded.AccountCircle,
        fabIcon = Icons.Rounded.ArrowForward,
        onFabClick = { _, navController ->
            navController.navigate(GuideRoutes.Update.route)
        }
    )

    class Update(title: String = "") : GuideUiState(
        title = title,
        icon = Icons.Rounded.Notifications,
        fabIcon = Icons.Rounded.ArrowForward,
        onFabClick = { _, navController ->
            navController.navigate(GuideRoutes.Env.route)
        }
    )

    class Env(
        title: String = "",
        fabIcon: ImageVector,
        onFabClick: (scaffoldNavController: NavHostController, navController: NavHostController) -> Unit
    ) :
        GuideUiState(
            title = title,
            icon = Icons.Rounded.CheckCircle,
            fabIcon = fabIcon,
            onFabClick = onFabClick
        )
}

@HiltViewModel
class GuideViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf<GuideUiState>(GuideUiState.Intro())
    val uiState: State<GuideUiState>
        get() = _uiState

    fun toUiState(uiState: GuideUiState) {
        _uiState.value = uiState
    }
}
