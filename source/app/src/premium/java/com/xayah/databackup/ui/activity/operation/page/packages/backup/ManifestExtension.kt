package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.cloud.setRemotePath
import com.xayah.databackup.ui.component.ListItemManifestVertical
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.command.Rclone
import com.xayah.databackup.util.readCloudActiveName
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.librootservice.service.RemoteRootService
import com.xayah.librootservice.util.withMainContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun OverlookExtensionItems() {
    val viewModel = hiltViewModel<ManifestViewModel>()
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState
    val context = LocalContext.current
    val name by context.readCloudActiveName().collectAsState(initial = "")
    val baseEntity by uiState.cloudDao.queryBaseByNameFlow(name).collectAsState(initial = null)
    val backupSavePath = baseEntity?.backupSavePath ?: stringResource(id = R.string.none)

    ListItemManifestVertical(
        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
        title = stringResource(R.string.backup_dir),
        content = backupSavePath
    ) {
        scope.launch {
            PickYouLauncher().setRemotePath(context = (context as ComponentActivity), rootService = RemoteRootService(context), name = name, scope = scope) {
                // Add "${name}:"
                val finalPath = "${name}:${it}"
                if (baseEntity != null) {
                    uiState.cloudDao.upsertBase(baseEntity!!.copy(backupSavePath = finalPath))
                }
            }
        }

    }
}

suspend fun manifestOnFabClickExtension(context: Context, logUtil: LogUtil, onSuccess: suspend () -> Unit) {
    val logId = logUtil.log("Cloud", "Test server.")
    val name = context.readCloudActiveName().first().toString()
    Rclone.mkdir(dst = "$name:DataBackupCloudTmpTest", dryRun = true).also { result ->
        result.logCmd(logUtil, logId)
        if (result.isSuccess) onSuccess()
        else withMainContext {
            Toast.makeText(context, context.getString(R.string.cloud_server_disconnected), Toast.LENGTH_SHORT).show()
        }
    }
}
