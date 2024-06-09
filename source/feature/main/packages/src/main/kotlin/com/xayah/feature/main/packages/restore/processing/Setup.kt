package com.xayah.feature.main.packages.restore.processing

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.datastore.KeyAutoScreenOff
import com.xayah.core.datastore.KeyResetRestoreList
import com.xayah.core.datastore.saveRestoreUser
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.PackageIcons
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.select
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.getValue
import com.xayah.core.ui.util.value
import com.xayah.feature.main.packages.ProcessingSetupScaffold
import com.xayah.feature.main.packages.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesRestoreProcessingSetup(localNavController: NavHostController, viewModel: IndexViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(null) {
        viewModel.launchOnIO {
            viewModel.emitIntent(IndexUiIntent.GetUsers)
            viewModel.emitIntent(IndexUiIntent.UpdateApps)
        }
    }

    ProcessingSetupScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = StringResourceToken.fromStringId(R.string.setup),
        actions = {
            Button(
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.FinishSetup(navController = localNavController))
                }) {
                Text(text = StringResourceToken.fromStringId(R.string._continue).value)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            Title(title = StringResourceToken.fromStringId(R.string.storage)) {
                val interactionSource = remember { MutableInteractionSource() }
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.apps),
                    value = StringResourceToken.fromString(uiState.packagesSize),
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_apps),
                    interactionSource = interactionSource,
                    content = {
                        PackageIcons(modifier = Modifier.paddingTop(SizeTokens.Level8), packages = uiState.packages, interactionSource = interactionSource) {}
                    }
                )
            }
            Title(title = StringResourceToken.fromStringId(R.string.settings)) {
                val dialogState = LocalSlotScope.current!!.dialogSlot
                val context = LocalContext.current
                var currentIndex by remember { mutableIntStateOf(0) }
                LaunchedEffect(currentIndex) {
                    viewModel.launchOnIO {
                        var userId = -1
                        if (currentIndex != 0) userId = uiState.restoreUsers[currentIndex].title.getValue(context).toIntOrNull() ?: -1
                        context.saveRestoreUser(userId)
                    }
                }
                Selectable(
                    enabled = uiState.restoreUsers.size != 1,
                    title = StringResourceToken.fromStringId(R.string.restore_user),
                    value = StringResourceToken.fromStringId(R.string.restore_user_desc),
                    current = uiState.restoreUsers[currentIndex].title,
                ) {
                    viewModel.launchOnIO {
                        val (state, selectedIndex) = dialogState.select(
                            title = StringResourceToken.fromStringId(R.string.restore_user),
                            defIndex = currentIndex,
                            items = uiState.restoreUsers
                        )
                        if (state) {
                            currentIndex = selectedIndex
                        }
                    }
                }
                Switchable(
                    key = KeyAutoScreenOff,
                    defValue = false,
                    title = StringResourceToken.fromStringId(R.string.auto_screen_off),
                    checkedText = StringResourceToken.fromStringId(R.string.auto_screen_off_desc),
                )
                Switchable(
                    key = KeyResetRestoreList,
                    defValue = false,
                    title = StringResourceToken.fromStringId(R.string.reset_restore_list),
                    checkedText = StringResourceToken.fromStringId(R.string.reset_restore_list_desc),
                )
            }
        }
    }
}
