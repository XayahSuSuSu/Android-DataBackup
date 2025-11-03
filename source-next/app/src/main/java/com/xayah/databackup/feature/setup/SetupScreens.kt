package com.xayah.databackup.feature.setup

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.OnResume
import com.xayah.databackup.ui.component.PermissionCard
import com.xayah.databackup.ui.component.rememberCallLogPermissionsState
import com.xayah.databackup.ui.component.rememberContactPermissionsState
import com.xayah.databackup.ui.component.rememberMessagePermissionsState
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.CustomSuFile
import com.xayah.databackup.util.KeyCustomSuFile
import com.xayah.databackup.util.ProcessHelper
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.readString
import com.xayah.databackup.util.saveString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun WelcomeScreen(navController: NavHostController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .height(IntrinsicSize.Max)
        ) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterVertically)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier.size(300.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.img_setup),
                        contentDescription = null
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(R.string.app_name),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    modifier = Modifier
                        .padding(horizontal = 48.dp)
                        .fillMaxWidth(),
                    text = stringResource(R.string.welcome_screen_app_desc),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier
                            .wrapContentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            navController.navigateSafely(Permissions(true))
                        }
                    ) {
                        Text(text = stringResource(R.string.get_started))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
fun CustomSUFileDialog(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(runBlocking { context.readString(CustomSuFile).first() }))
    }
    var isError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        title = { Text(text = stringResource(R.string.custom_su_file)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    isError = it.text.isBlank()
                    text = it
                },
                isError = isError,
                label = { Text(text = stringResource(R.string.file)) },
                supportingText = { Text(text = stringResource(R.string.restart_to_take_effect)) }
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                enabled = isError.not(),
                onClick = {
                    scope.launch(Dispatchers.Default) {
                        context.saveString(KeyCustomSuFile, text.text)
                        onDismissRequest()
                        ProcessHelper.killSelf(context as Activity)
                    }
                }
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.dismiss))
            }
        }
    )
}

@Composable
fun PermissionsScreen(
    navController: NavHostController,
    viewModel: PermissionsViewModel = viewModel(),
    permissions: Permissions,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var openCustomSUFileDialog by remember { mutableStateOf(false) }
    val contactsPermissionState = rememberContactPermissionsState { result ->
        viewModel.viewModelScope.launch {
            viewModel.checkContact(context, result)
        }
    }
    val callLogsPermissionState = rememberCallLogPermissionsState { result ->
        viewModel.viewModelScope.launch {
            viewModel.checkCallLog(context, result)
        }
    }
    val messagesPermissionState = rememberMessagePermissionsState { result ->
        viewModel.viewModelScope.launch {
            viewModel.checkMessage(context, result)
        }
    }

    OnResume {
        if (viewModel.mIsGrantingNotificationPermission) {
            viewModel.viewModelScope.launch {
                viewModel.checkNotification(context)
                viewModel.mIsGrantingNotificationPermission = false
            }
        }
    }

    if (openCustomSUFileDialog) {
        CustomSUFileDialog {
            openCustomSUFileDialog = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
            ) {
                Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    text = stringResource(R.string.permissions),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    modifier = Modifier
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                    text = stringResource(R.string.permissions_screen_desc),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.required),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )

                    PermissionCard(
                        state = uiState.rootCardProp.state,
                        icon = ImageVector.vectorResource(uiState.rootCardProp.icon),
                        title = uiState.rootCardProp.title,
                        content = uiState.rootCardProp.content,
                        onClick = {
                            viewModel.withLock {
                                viewModel.validateRoot(context)
                            }
                        },
                        actionIcon = ImageVector.vectorResource(R.drawable.ic_settings),
                        actionIconDescription = stringResource(R.string.custom_su_file),
                        onActionButtonClick = {
                            openCustomSUFileDialog = true
                        }
                    )

                    PermissionCard(
                        state = uiState.notificationProp.state,
                        icon = ImageVector.vectorResource(uiState.notificationProp.icon),
                        title = uiState.notificationProp.title,
                        content = uiState.notificationProp.content,
                        onClick = {
                            viewModel.withLock {
                                viewModel.validateNotification(context)
                            }
                        },
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.optional),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )

                    PermissionCard(
                        state = uiState.contactProp.state,
                        icon = ImageVector.vectorResource(uiState.contactProp.icon),
                        title = uiState.contactProp.title,
                        content = uiState.contactProp.content,
                        onClick = {
                            viewModel.withLock {
                                viewModel.validateContact(contactsPermissionState)
                            }
                        },
                    )

                    PermissionCard(
                        state = uiState.callLogProp.state,
                        icon = ImageVector.vectorResource(uiState.callLogProp.icon),
                        title = uiState.callLogProp.title,
                        content = uiState.callLogProp.content,
                        onClick = {
                            viewModel.withLock {
                                viewModel.validateCallLog(callLogsPermissionState)
                            }
                        },
                    )

                    PermissionCard(
                        state = uiState.messageProp.state,
                        icon = ImageVector.vectorResource(uiState.messageProp.icon),
                        title = uiState.messageProp.title,
                        content = uiState.messageProp.content,
                        onClick = {
                            viewModel.withLock {
                                viewModel.validateMessage(messagesPermissionState)
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = permissions.enableBackBtn,
                    modifier = Modifier
                        .wrapContentSize(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        navController.popBackStackSafely()
                    }
                ) {
                    Text(text = stringResource(R.string.back))
                }

                Spacer(modifier = Modifier.weight(1f))

                FadeVisibility(visible = viewModel.misRequiredGranted && viewModel.misAllGranted.not()) {
                    TextButton(
                        enabled = permissions.enableBackBtn,
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(end = 16.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        onClick = { viewModel.onSkipButtonClick(context) }
                    ) {
                        Text(text = stringResource(R.string.skip))
                    }
                }

                Button(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateContentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = { viewModel.onNextButtonClick(context, contactsPermissionState, callLogsPermissionState, messagesPermissionState) }
                ) {
                    AnimatedContent(
                        targetState = if (viewModel.misAllGranted) stringResource(R.string.next) else stringResource(R.string.grant_all),
                        label = "Animated content"
                    ) { targetContent ->
                        Text(text = targetContent)
                    }
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}
