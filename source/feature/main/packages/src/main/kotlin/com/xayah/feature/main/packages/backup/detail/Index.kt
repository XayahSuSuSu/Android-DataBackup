package com.xayah.feature.main.packages.backup.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.DataType
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.joinOf
import com.xayah.core.util.DateUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.feature.main.packages.ListScaffold
import com.xayah.feature.main.packages.R
import com.xayah.feature.main.packages.countItems
import com.xayah.feature.main.packages.reversePermission
import com.xayah.feature.main.packages.reverseSsaid
import com.xayah.feature.main.packages.reversedPackage

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PagePackagesBackupDetail() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val packageState by viewModel.packageState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.OnRefresh)
    }

    ListScaffold(
        scrollBehavior = scrollBehavior,
        title = stringResource(id = R.string.details),
        actions = {}
    ) {
        packageState?.also { pkg ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .paddingHorizontal(SizeTokens.Level24)
                        .paddingVertical(SizeTokens.Level12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
                ) {
                    PackageIconImage(
                        packageName = uiState.packageName,
                        size = SizeTokens.Level64
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        TitleLargeText(text = pkg.packageInfo.label, color = ColorSchemeKeyTokens.OnSurface.toColor())
                        BodyMediumText(text = uiState.packageName, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor())
                    }
                }
                Title(title = stringResource(id = R.string.info)) {
                    Clickable(
                        title = stringResource(id = R.string.user),
                        value = uiState.userId.toString(),
                    )
                    Clickable(
                        title = stringResource(id = R.string.uid),
                        value = pkg.extraInfo.uid.toString()
                    )
                    Clickable(
                        title = stringResource(id = R.string.version),
                        value = pkg.packageInfo.versionName,
                    )
                    Clickable(
                        title = stringResource(id = R.string.first_install),
                        value = DateUtil.formatTimestamp(
                            pkg.packageInfo.firstInstallTime,
                            "yyyy-MM-dd"
                        ),
                    )
                }
                Title(title = stringResource(id = R.string.backup_parts)) {
                    Switchable(
                        checked = pkg.permissionSelected,
                        title = stringResource(id = R.string.permissions),
                        checkedText = countItems(context, pkg.extraInfo.permissions.size),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversePermission()))
                    }
                    Switchable(
                        enabled = pkg.extraInfo.ssaid.isNotEmpty(),
                        checked = pkg.ssaidSelected,
                        title = stringResource(id = R.string.ssaid),
                        checkedText = pkg.extraInfo.ssaid.ifEmpty { stringResource(id = R.string.none) },
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reverseSsaid()))
                    }
                    Switchable(
                        checked = pkg.apkSelected,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_android),
                        title = DataType.PACKAGE_APK.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                pkg.displayStats.apkBytes.toDouble().formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            pkg.displayStats.apkBytes.toDouble().formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversedPackage(DataType.PACKAGE_APK)))
                    }
                    Switchable(
                        checked = pkg.userSelected,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_person),
                        title = DataType.PACKAGE_USER.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                pkg.displayStats.userBytes.toDouble().formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            pkg.displayStats.userBytes.toDouble().formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversedPackage(DataType.PACKAGE_USER)))
                    }
                    Switchable(
                        checked = pkg.userDeSelected,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_manage_accounts),
                        title = DataType.PACKAGE_USER_DE.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                pkg.displayStats.userDeBytes.toDouble().formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            pkg.displayStats.userDeBytes.toDouble().formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversedPackage(DataType.PACKAGE_USER_DE)))
                    }
                    Switchable(
                        checked = pkg.dataSelected,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_database),
                        title = DataType.PACKAGE_DATA.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                pkg.displayStats.dataBytes.toDouble().formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            pkg.displayStats.dataBytes.toDouble().formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversedPackage(DataType.PACKAGE_DATA)))
                    }
                    Switchable(
                        checked = pkg.obbSelected,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_stadia_controller),
                        title = DataType.PACKAGE_OBB.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                pkg.displayStats.obbBytes.toDouble().formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            pkg.displayStats.obbBytes.toDouble().formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversedPackage(DataType.PACKAGE_OBB)))
                    }
                    Switchable(
                        checked = pkg.mediaSelected,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_image),
                        title = DataType.PACKAGE_MEDIA.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                pkg.displayStats.mediaBytes.toDouble().formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            pkg.displayStats.mediaBytes.toDouble().formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdatePackage(pkg.reversedPackage(DataType.PACKAGE_MEDIA)))
                    }
                }
            }
        }
    }
}
