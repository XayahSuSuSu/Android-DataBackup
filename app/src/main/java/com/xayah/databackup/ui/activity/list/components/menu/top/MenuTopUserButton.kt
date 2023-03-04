package com.xayah.databackup.ui.activity.list.components.menu.top

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.list.ListViewModel
import com.xayah.databackup.ui.activity.settings.components.content.onBackupUserPrepare
import com.xayah.databackup.ui.activity.settings.components.content.onRestoreUserPrepare
import com.xayah.databackup.ui.activity.settings.components.dialog.RadioButtonTextDialog
import com.xayah.databackup.ui.components.LoadingDialog
import com.xayah.databackup.util.*
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun MenuTopBackupUserButton(viewModel: ListViewModel, onInitialize: suspend () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val icon = ImageVector.vectorResource(id = R.drawable.ic_round_person)
    val title = stringResource(R.string.backup_user)

    val contentState = remember {
        mutableStateOf(context.readBackupUser())
    }
    val isDialogOpen = remember {
        mutableStateOf(false)
    }
    val items = remember {
        mutableStateOf(listOf(""))
    }
    val selected = remember {
        mutableStateOf("")
    }

    val isLoadingDialogOpen = remember {
        mutableStateOf(false)
    }
    LoadingDialog(isOpen = isLoadingDialogOpen)

    RadioButtonTextDialog(
        isOpen = isDialogOpen,
        icon = icon,
        title = title,
        items = items.value,
        selected = selected,
        onConfirm = {
            try {
                val currentUserId = contentState.value.split(":")[0]
                val selectedUserId = items.value[it].split(":")[0]
                if (currentUserId != selectedUserId) {
                    contentState.value = items.value[it]
                    scope.launch {
                        isLoadingDialogOpen.value = true
                        GlobalObject.getInstance().appInfoBackupMap.value.clear()
                        GlobalObject.getInstance().appInfoRestoreMap.value.clear()
                        viewModel.appBackupList.value.clear()
                        viewModel.appRestoreList.value.clear()
                        context.saveBackupUser(selectedUserId)
                        onInitialize()
                        isLoadingDialogOpen.value = false
                    }
                }
            } catch (_: Exception) {
            }

        }
    )

    MenuTopActionButton(
        icon = icon,
        title = title
    ) {
        scope.launch {
            val (list, value) = onBackupUserPrepare(context)
            items.value = list
            selected.value = value
            isDialogOpen.value = true
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun MenuTopRestoreUserButton(viewModel: ListViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val icon = ImageVector.vectorResource(id = R.drawable.ic_round_iphone)
    val title = stringResource(R.string.restore_user)

    val contentState = remember {
        mutableStateOf(context.readRestoreUser())
    }
    val isDialogOpen = remember {
        mutableStateOf(false)
    }
    val items = remember {
        mutableStateOf(listOf(""))
    }
    val selected = remember {
        mutableStateOf("")
    }

    RadioButtonTextDialog(
        isOpen = isDialogOpen,
        icon = icon,
        title = title,
        items = items.value,
        selected = selected,
        onConfirm = {
            try {
                val selectedUserId = items.value[it].split(":")[0]
                contentState.value = items.value[it]
                context.saveRestoreUser(selectedUserId)
            } catch (_: Exception) {
            }
        }
    )

    MenuTopActionButton(
        icon = icon,
        title = title
    ) {
        scope.launch {
            val (list, value) = onRestoreUserPrepare(context)
            items.value = list
            selected.value = value
            isDialogOpen.value = true
        }
    }
}