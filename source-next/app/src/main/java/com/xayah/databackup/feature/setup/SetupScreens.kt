package com.xayah.databackup.feature.setup

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.CustomSUFileDialog
import com.xayah.databackup.ui.component.OnResume
import com.xayah.databackup.ui.component.PermissionCard
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(navigator: Navigator) {
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
                            navigator.navigateSafely(Permissions(true))
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
fun PermissionsScreen(
    navigator: Navigator,
    viewModel: PermissionsViewModel = viewModel(),
    permissions: Permissions,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var openCustomSUFileDialog by remember { mutableStateOf(false) }
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
                    SectionHeader(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.required),
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
                        navigator.popBackStackSafely()
                    }
                ) {
                    Text(text = stringResource(R.string.back))
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier
                        .wrapContentSize()
                        .animateContentSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = { viewModel.onNextButtonClick(context) }
                ) {
                    AnimatedContent(
                        targetState = if (viewModel.isRequiredGranted) stringResource(R.string.next) else stringResource(R.string.grant_all),
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
