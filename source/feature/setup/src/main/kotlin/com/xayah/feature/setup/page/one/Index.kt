package com.xayah.feature.setup.page.one

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readCustomSUFile
import com.xayah.core.datastore.saveCustomSUFile
import com.xayah.core.ui.component.AppIcon
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.HeadlineMediumText
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Section
import com.xayah.core.ui.component.SetOnResume
import com.xayah.core.ui.component.edit
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.navigateSingle
import com.xayah.feature.setup.PermissionButton
import com.xayah.feature.setup.R
import com.xayah.feature.setup.SetupRoutes
import com.xayah.feature.setup.SetupScaffold
import kotlinx.coroutines.flow.first

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageOne() {
    val navController = LocalNavController.current!!
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rootState by viewModel.rootState.collectAsStateWithLifecycle()
    val abiState by viewModel.abiState.collectAsStateWithLifecycle()
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()
    val allRequiredValidated by viewModel.allRequiredValidated.collectAsStateWithLifecycle()
    val allOptionalValidated by viewModel.allOptionalValidated.collectAsStateWithLifecycle()
    val dialogState = LocalSlotScope.current!!.dialogSlot

    SetOnResume {
        viewModel.emitIntentOnIO(IndexUiIntent.OnResume)
    }

    SetupScaffold(
        actions = {
            AnimatedVisibility(visible = allRequiredValidated.not() || allOptionalValidated.not()) {
                OutlinedButton(
                    onClick = {
                        viewModel.launchOnIO {
                            viewModel.emitIntent(IndexUiIntent.ValidateRoot)
                            viewModel.emitIntent(IndexUiIntent.ValidateAbi)
                            viewModel.emitIntent(IndexUiIntent.ValidateNotification(context = context))
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.grant_all))
                }
            }
            Button(enabled = allRequiredValidated, onClick = { navController.navigateSingle(SetupRoutes.Two.route) }) {
                Text(text = stringResource(id = R.string._continue))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Column(
                modifier = Modifier
                    .paddingTop(SizeTokens.Level100),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIcon()
                HeadlineMediumText(modifier = Modifier.paddingTop(SizeTokens.Level12), text = stringResource(id = R.string.welcome_to_use))
                BodyMediumText(text = stringResource(id = R.string.app_short_desc), color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
            }

            Spacer(modifier = Modifier.size(SizeTokens.Level24))

            Section(title = stringResource(id = R.string.required)) {
                PermissionButton(
                    title = stringResource(id = R.string.root_permission),
                    desc = stringResource(id = R.string.root_permission_desc),
                    envState = rootState,
                    onSetting = {
                        viewModel.launchOnIO {
                            val (state, su) = dialogState.edit(
                                title = context.getString(R.string.custom_su_file),
                                defValue = context.readCustomSUFile().first(),
                                label = context.getString(R.string.name),
                                desc = context.getString(R.string.restart_to_take_effect)
                            )
                            if (state.isConfirm) {
                                context.saveCustomSUFile(su)
                            }
                        }
                    }
                ) {
                    viewModel.launchOnIO {
                        viewModel.emitIntent(IndexUiIntent.ValidateRoot)
                    }
                }
                PermissionButton(
                    title = stringResource(id = R.string.abi_validation),
                    desc = uiState.abiErr.ifEmpty { context.getString(R.string.abi_validation_desc) },
                    envState = abiState,
                ) {
                    viewModel.launchOnIO {
                        viewModel.emitIntent(IndexUiIntent.ValidateAbi)
                    }
                }
            }

            Section(title = stringResource(id = R.string.optional)) {
                PermissionButton(
                    title = stringResource(id = R.string.notification_permission),
                    desc = stringResource(id = R.string.notification_permission_desc),
                    envState = notificationState,
                ) {
                    viewModel.launchOnIO {
                        viewModel.emitIntent(IndexUiIntent.ValidateNotification(context = context))
                    }
                }
            }
        }
    }
}
