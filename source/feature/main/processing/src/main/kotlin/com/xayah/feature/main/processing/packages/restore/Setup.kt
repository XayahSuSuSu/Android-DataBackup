package com.xayah.feature.main.processing.packages.restore

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.component.select
import com.xayah.core.ui.token.SizeTokens
import com.xayah.feature.main.processing.FinishSetup
import com.xayah.feature.main.processing.GetUsers
import com.xayah.feature.main.processing.ProcessingSetupScaffold
import com.xayah.feature.main.processing.R
import com.xayah.feature.main.processing.SetCloudEntity
import com.xayah.feature.main.processing.UpdateApps
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesRestoreProcessingSetup(localNavController: NavHostController, viewModel: RestoreViewModelImpl) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val packagesSize by viewModel.packagesSize.collectAsStateWithLifecycle()
    val restoreUsers by viewModel.restoreUsers.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.launchOnIO {
            viewModel.emitIntent(SetCloudEntity(""))
            viewModel.emitIntent(GetUsers)
            viewModel.emitIntent(UpdateApps)
        }
    }

    ProcessingSetupScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.setup),
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level8),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
            ) {
                Button(
                    onClick = {
                        viewModel.emitIntentOnIO(FinishSetup(navController = localNavController))
                    }) {
                    Text(text = stringResource(id = R.string._continue))
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            Title(title = stringResource(id = R.string.storage)) {
                val interactionSource = remember { MutableInteractionSource() }
                Clickable(
                    title = stringResource(id = R.string.apps),
                    value = packagesSize,
                    leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_apps),
                    interactionSource = interactionSource,
                    content = {
                        PackageIcons(modifier = Modifier.paddingTop(SizeTokens.Level8), packages = packages)
                    }
                )
            }
            Title(title = stringResource(id = R.string.settings)) {
                val dialogState = LocalSlotScope.current!!.dialogSlot
                val context = LocalContext.current
                var currentIndex by remember { mutableIntStateOf(0) }
                LaunchedEffect(currentIndex) {
                    viewModel.launchOnIO {
                        var userId = -1
                        if (currentIndex != 0) userId = restoreUsers[currentIndex].title.toIntOrNull() ?: -1
                        context.saveRestoreUser(userId)
                    }
                }
                Selectable(
                    enabled = restoreUsers.size != 1,
                    title = stringResource(id = R.string.restore_user),
                    value = stringResource(id = R.string.restore_user_desc),
                    current = restoreUsers[currentIndex].title,
                ) {
                    viewModel.launchOnIO {
                        val (state, selectedIndex) = dialogState.select(
                            title = context.getString(R.string.restore_user),
                            defIndex = currentIndex,
                            items = restoreUsers
                        )
                        if (state.isConfirm) {
                            currentIndex = selectedIndex
                        }
                    }
                }
                Switchable(
                    key = KeyAutoScreenOff,
                    defValue = false,
                    title = stringResource(id = R.string.auto_screen_off),
                    checkedText = stringResource(id = R.string.auto_screen_off_desc),
                )
                Switchable(
                    key = KeyResetRestoreList,
                    defValue = false,
                    title = stringResource(id = R.string.reset_restore_list),
                    checkedText = stringResource(id = R.string.reset_restore_list_desc),
                )
            }
        }
    }
}
