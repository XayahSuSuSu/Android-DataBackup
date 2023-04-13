package com.xayah.databackup.ui.activity.guide.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.ui.activity.guide.GuideViewModel
import com.xayah.databackup.ui.activity.guide.components.card.CardEnvironment
import com.xayah.databackup.ui.components.TextDialog
import com.xayah.databackup.util.Path
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun PageEnvironment(
    viewModel: GuideViewModel,
    onPass: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPermissionDialogOpen = viewModel.isPermissionDialogOpen
    val showFinishBtn = viewModel.showFinishBtn
    val environmentList = viewModel.environmentList.collectAsState()

    LaunchedEffect(null) {
        if (viewModel.environmentList.value.isEmpty())
            viewModel.environmentList.value = mutableStateListOf(
                ItemEnvironment(
                    itemId = R.string.grant_root_access,
                    cardState = mutableStateOf(LoadingState.Loading),
                    onCheck = {
                        viewModel.checkRootAccess()
                    }
                ),
                ItemEnvironment(
                    itemId = R.string.release_prebuilt_binaries,
                    cardState = mutableStateOf(LoadingState.Loading),
                    onCheck = {
                        val state = viewModel.binRelease(context)
                        if (state == LoadingState.Failed)
                            isPermissionDialogOpen.value = true
                        state
                    }
                )
            )
    }

    GuideScaffold(
        title = stringResource(id = R.string.environment_detection),
        icon = Icons.Rounded.CheckCircle,
        showBtnIcon = showFinishBtn.value,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = {
            onPass()
        },
        items = {
            items(items = environmentList.value, key = { it.itemId }) {
                CardEnvironment(
                    item = stringResource(id = it.itemId),
                    cardState = it.cardState.value,
                    onCardClick = {
                        scope.launch {
                            val state = it.onCheck()
                            it.cardState.value = state
                            viewModel.checkAllGranted()
                        }
                    }
                )
            }
        }
    )

    TextDialog(
        isOpen = isPermissionDialogOpen,
        icon = Icons.Rounded.Warning,
        title = stringResource(id = R.string.error),
        content = "${stringResource(R.string.path_permission_error)}:" + "\n" +
                "rwxrwxrwx(777): ${Path.getAppInternalFilesPath()}/bin",
        confirmText = stringResource(R.string.copy_grant_command),
        onConfirmClick = {
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText(
                    "PermissionCmd",
                    "su; chmod 777 ${Path.getAppInternalFilesPath()}/bin/*"
                )
            )
            isPermissionDialogOpen.value = false
        },
        showDismissBtn = false
    )
}
