package com.xayah.feature.main.cloud.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.SFTPAuthMode
import com.xayah.core.model.database.SFTPExtra
import com.xayah.core.model.util.indexOf
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.FullscreenModalBottomSheet
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.cloud.AccountSetupScaffold
import com.xayah.feature.main.cloud.R
import com.xayah.feature.main.cloud.SetupTextField
import kotlinx.coroutines.launch

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageSFTPSetup() {
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var name by rememberSaveable { mutableStateOf(uiState.currentName) }
    var remote by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.remote ?: "") }
    var url by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.host ?: "") }
    var port by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.getExtraEntity<SFTPExtra>()?.port?.toString() ?: "22") }
    var username by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.user ?: "") }
    val modeOptions = stringArrayResource(id = R.array.sftp_auth_mode).toList()
    var modeIndex by rememberSaveable(uiState.cloudEntity) { mutableIntStateOf(uiState.cloudEntity?.getExtraEntity<SFTPExtra>()?.mode?.index ?: 0) }
    var password by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.pass ?: "") }
    var privateKey by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.getExtraEntity<SFTPExtra>()?.privateKey ?: "") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val allFilled by rememberSaveable(
        name,
        url,
        port,
        username,
        password,
        privateKey,
        modeIndex
    ) { mutableStateOf(
        name.isNotEmpty() &&
                url.isNotEmpty() &&
                port.isNotEmpty() &&
                username.isNotEmpty() &&
                ((modeIndex == 0 && password.isNotEmpty()) || modeIndex == 1) &&
                ((modeIndex == 1 && privateKey.isNotEmpty()) || modeIndex == 0)
    ) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Initialize)
    }

    AccountSetupScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = StringResourceToken.fromStringId(R.string.sftp_setup),
        actions = {
            TextButton(
                enabled = allFilled && uiState.isProcessing.not(),
                onClick = {
                    viewModel.launchOnIO {
                        viewModel.updateSFTPEntity(
                            name = name,
                            remote = remote,
                            url = url,
                            username = username,
                            password = password,
                            port = port,
                            mode = SFTPAuthMode.indexOf(modeIndex),
                            privateKey = privateKey,
                        )
                        viewModel.emitIntent(IndexUiIntent.TestConnection)
                    }
                }
            ) {
                Text(text = StringResourceToken.fromStringId(R.string.test_connection).value)
            }

            Button(enabled = allFilled && remote.isNotEmpty() && uiState.isProcessing.not(), onClick = {
                viewModel.launchOnIO {
                    viewModel.updateSFTPEntity(
                        name = name,
                        remote = remote,
                        url = url,
                        username = username,
                        password = password,
                        port = port,
                        mode = SFTPAuthMode.indexOf(modeIndex),
                        privateKey = privateKey,
                    )
                    viewModel.emitIntent(IndexUiIntent.CreateAccount(navController = navController))
                }
            }) {
                Text(text = StringResourceToken.fromStringId(R.string._continue).value)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Title(enabled = uiState.isProcessing.not(), title = StringResourceToken.fromStringId(R.string.server), verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)) {
                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.currentName.isEmpty() && uiState.isProcessing.not(),
                    value = name,
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_badge),
                    onValueChange = { name = it },
                    label = StringResourceToken.fromStringId(R.string.name)
                )

                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    value = url,
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_link),
                    prefix = StringResourceToken.fromString("sftp://"),
                    onValueChange = { url = it },
                    label = StringResourceToken.fromStringId(R.string.url)
                )

                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    value = port,
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_lan),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { port = it },
                    label = StringResourceToken.fromStringId(R.string.port)
                )
            }

            Title(enabled = uiState.isProcessing.not(), title = StringResourceToken.fromStringId(R.string.account), verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                ) {
                    modeOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            enabled = uiState.isProcessing.not(),
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modeOptions.size),
                            onClick = {
                                password = ""
                                modeIndex = index
                            },
                            selected = index == modeIndex
                        ) {
                            Text(label)
                        }
                    }
                }

                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    value = username,
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
                    onValueChange = { username = it },
                    label = StringResourceToken.fromStringId(R.string.username)
                )

                AnimatedVisibility(modeIndex == 1) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    var showBottomSheet by remember { mutableStateOf(false) }

                    SetupTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(SizeTokens.Level24),
                        enabled = true,
                        readOnly = true,
                        value = if (privateKey.isEmpty()) "" else "[ Private key ]",
                        leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.VpnKey),
                        onValueChange = { },
                        label = StringResourceToken.fromStringId(R.string.private_key),
                        onClick = { showBottomSheet = true }
                    )

                    if (showBottomSheet) {
                        FullscreenModalBottomSheet(
                            onDismissRequest = {
                                showBottomSheet = false
                            },
                            sheetState = sheetState,
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(SizeTokens.Level24),
                                value = privateKey,
                                onValueChange = {
                                    privateKey = it
                                },
                                label = { Text(text = StringResourceToken.fromStringId(R.string.private_key).value) },
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .paddingHorizontal(SizeTokens.Level24)
                                    .paddingBottom(SizeTokens.Level24),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(enabled = true, onClick = {
                                    scope.launch {
                                        sheetState.hide()
                                        showBottomSheet = false
                                    }
                                }) {
                                    Text(text = StringResourceToken.fromStringId(R.string.confirm).value)
                                }
                            }
                            InnerBottomSpacer(innerPadding = it)
                        }
                    }
                }

                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    value = password,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_key),
                    trailingIcon = ImageVectorToken.fromVector(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff),
                    onTrailingIconClick = {
                        passwordVisible = passwordVisible.not()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    onValueChange = { password = it },
                    label = if (modeIndex == 1) StringResourceToken.fromStringArgs(
                        StringResourceToken.fromStringId(R.string.password),
                        StringResourceToken.fromString("("),
                        StringResourceToken.fromStringId(R.string.allow_empty),
                        StringResourceToken.fromString(")"),
                    ) else StringResourceToken.fromStringId(R.string.password),
                )
            }

            Title(enabled = uiState.isProcessing.not(), title = StringResourceToken.fromStringId(R.string.advanced)) {
                Clickable(
                    enabled = allFilled && uiState.isProcessing.not(),
                    title = StringResourceToken.fromStringId(R.string.remote_path),
                    value = StringResourceToken.fromString(remote.ifEmpty { context.getString(R.string.not_selected) }),
                    desc = StringResourceToken.fromStringId(R.string.remote_path_desc),
                ) {
                    viewModel.launchOnIO {
                        viewModel.updateSFTPEntity(
                            name = name,
                            remote = remote,
                            url = url,
                            username = username,
                            password = password,
                            port = port,
                            mode = SFTPAuthMode.indexOf(modeIndex),
                            privateKey = privateKey,
                        )
                        viewModel.emitIntent(IndexUiIntent.SetRemotePath(context = context))
                        remote = uiState.cloudEntity!!.remote
                    }
                }

                if (uiState.currentName.isNotEmpty())
                    TextButton(
                        modifier = Modifier
                            .paddingStart(SizeTokens.Level12)
                            .paddingTop(SizeTokens.Level12),
                        enabled = uiState.isProcessing.not(),
                        onClick = {
                            viewModel.launchOnIO {
                                if (dialogState.confirm(title = StringResourceToken.fromStringId(R.string.delete_account), text = StringResourceToken.fromStringId(R.string.delete_account_desc))) {
                                    viewModel.emitIntent(IndexUiIntent.DeleteAccount(navController = navController))
                                }
                            }
                        }
                    ) {
                        Text(
                            text = StringResourceToken.fromStringId(R.string.delete_account).value,
                            color = ColorSchemeKeyTokens.Error.toColor(uiState.isProcessing.not())
                        )
                    }
            }
        }
    }
}
