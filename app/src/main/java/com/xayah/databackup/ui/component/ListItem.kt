package com.xayah.databackup.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toDrawable
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.data.PackageBackupEntire
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.ListItemTokens
import com.xayah.databackup.util.PathUtil
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch
import java.io.File
import com.xayah.databackup.ui.activity.operation.page.packages.backup.ListViewModel as BackupListViewModel
import com.xayah.databackup.ui.activity.operation.page.packages.restore.ListViewModel as RestoreListViewModel

@ExperimentalMaterial3Api
@Composable
fun ListItemPackage(
    modifier: Modifier = Modifier,
    packageName: String,
    label: String,
    icon: Any,
    apkSelected: Boolean,
    dataSelected: Boolean,
    onApkSelected: () -> Unit,
    onDataSelected: () -> Unit,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onCardClick,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium)
        ) {
            Row(
                modifier = Modifier
                    .paddingHorizontal(ListItemTokens.PaddingMedium)
                    .paddingTop(ListItemTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall)
            ) {
                AsyncImage(
                    modifier = Modifier.size(ListItemTokens.IconSize),
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    TitleMediumBoldText(text = label)
                    LabelSmallText(text = packageName)
                }
            }
            Row(
                modifier = Modifier.paddingHorizontal(ListItemTokens.PaddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                    content = {
                        chipGroup()
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = ColorScheme.inverseOnSurface())
                    .paddingHorizontal(ListItemTokens.PaddingMedium),
                horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingMedium, Alignment.End)
            ) {
                FilterChip(
                    selected = apkSelected,
                    onClick = onApkSelected,
                    label = { Text(stringResource(R.string.apk)) },
                    leadingIcon = if (apkSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
                FilterChip(
                    selected = dataSelected,
                    onClick = onDataSelected,
                    label = { Text(stringResource(R.string.data)) },
                    leadingIcon = if (dataSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun ListItemPackageBackup(
    modifier: Modifier = Modifier,
    packageInfo: PackageBackupEntire,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<BackupListViewModel>()
    val scope = rememberCoroutineScope()
    var icon by remember { mutableStateOf<Any>(0) }
    var apkSelected by remember { mutableStateOf(OperationMask.isApkSelected(packageInfo.operationCode)) }
    var dataSelected by remember { mutableStateOf(OperationMask.isDataSelected(packageInfo.operationCode)) }

    ListItemPackage(
        modifier = modifier,
        packageName = packageInfo.packageName,
        label = packageInfo.label,
        icon = icon,
        apkSelected = apkSelected,
        dataSelected = dataSelected,
        onApkSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Apk
                    apkSelected = OperationMask.isApkSelected(packageInfo.operationCode)
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        onDataSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Data
                    dataSelected = OperationMask.isDataSelected(packageInfo.operationCode)
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        onCardClick = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode =
                        if (packageInfo.operationCode == OperationMask.Both) OperationMask.None else OperationMask.Both
                    apkSelected = OperationMask.isApkSelected(packageInfo.operationCode)
                    dataSelected = OperationMask.isDataSelected(packageInfo.operationCode)
                    viewModel.updatePackage(packageInfo)
                }
            }
        }
    ) {
        Serial(serial = packageInfo.versionName)
        Serial(serial = packageInfo.sizeDisplay)
    }

    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            tryOn {
                val bytes = File(PathUtil.getIconPath(context, packageInfo.packageName)).readBytes()
                icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun ListItemPackageRestore(
    modifier: Modifier = Modifier,
    packageInfo: PackageRestoreEntire,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<RestoreListViewModel>()
    val scope = rememberCoroutineScope()
    var icon by remember { mutableStateOf<Any>(0) }
    var apkSelected by remember { mutableStateOf(OperationMask.isApkSelected(packageInfo.operationCode)) }
    var dataSelected by remember { mutableStateOf(OperationMask.isDataSelected(packageInfo.operationCode)) }

    ListItemPackage(
        modifier = modifier,
        packageName = packageInfo.packageName,
        label = packageInfo.label,
        icon = icon,
        apkSelected = apkSelected,
        dataSelected = dataSelected,
        onApkSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Apk
                    apkSelected = OperationMask.isApkSelected(packageInfo.operationCode)
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        onDataSelected = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode = packageInfo.operationCode xor OperationMask.Data
                    dataSelected = OperationMask.isDataSelected(packageInfo.operationCode)
                    viewModel.updatePackage(packageInfo)
                }
            }
        },
        onCardClick = {
            scope.launch {
                withIOContext {
                    packageInfo.operationCode =
                        if (packageInfo.operationCode == OperationMask.Both) OperationMask.None else OperationMask.Both
                    apkSelected = OperationMask.isApkSelected(packageInfo.operationCode)
                    dataSelected = OperationMask.isDataSelected(packageInfo.operationCode)
                    viewModel.updatePackage(packageInfo)
                }
            }
        }
    ) {
        Serial(serial = packageInfo.versionName)
    }

    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            tryOn {
                val bytes = File(PathUtil.getIconPath(context, packageInfo.packageName)).readBytes()
                icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources)
            }
        }
    }
}

@Composable
fun ListItemManifest(icon: ImageVector, title: String, content: String, onButtonClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(
            modifier = Modifier.size(ListItemTokens.ManifestIconButtonSize),
            onClick = onButtonClick
        ) {
            Icon(
                modifier = Modifier.size(ListItemTokens.ManifestIconSize),
                imageVector = icon,
                contentDescription = null
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(ListItemTokens.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleMediumBoldText(text = title)
            HeadlineLargeBoldText(text = content)
        }
    }
}
