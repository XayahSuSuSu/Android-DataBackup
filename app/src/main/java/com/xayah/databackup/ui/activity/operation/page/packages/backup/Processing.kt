package com.xayah.databackup.ui.activity.operation.page.packages.backup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.data.PackageBackupOperation
import com.xayah.databackup.ui.activity.main.router.navigateAndPopAllStack
import com.xayah.databackup.ui.activity.main.router.navigateAndPopBackStack
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.BodySmallBoldText
import com.xayah.databackup.ui.component.OperationCard
import com.xayah.databackup.ui.component.OperationCardConfig
import com.xayah.databackup.ui.component.ProcessingTopBar
import com.xayah.databackup.ui.component.SlotScope
import com.xayah.databackup.ui.component.TitleLargeBoldText
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.WaverImage
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.iconPath
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun SlotScope.PackageBackupProcessing() {
    val viewModel = hiltViewModel<ProcessingViewModel>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    var icon: Bitmap? by remember { mutableStateOf(null) }
    val latestPackage: PackageBackupOperation? by viewModel.uiState.value.latestPackage.collectAsState(null)
    val progressAnimation: Float by animateFloatAsState(
        latestPackage?.progress ?: 0f,
        label = AnimationTokens.AnimateFloatAsStateLabel,
        animationSpec = tween(durationMillis = AnimationTokens.TweenDuration)
    )
    val selectedBothCount by viewModel.uiState.value.selectedBothCount.collectAsState(initial = 0)
    val operationCount by viewModel.uiState.value.operationCount.collectAsState(initial = 0)
    val totalProgressAnimation: Float by animateFloatAsState(
        if (selectedBothCount == 0) 0f else (operationCount.toFloat() - 1).coerceAtLeast(0F) / selectedBothCount,
        label = AnimationTokens.AnimateFloatAsStateLabel,
        animationSpec = tween(durationMillis = AnimationTokens.TweenDuration)
    )
    val label = if (latestPackage == null) stringResource(R.string.idle) else latestPackage!!.label
    val packageName = if (latestPackage == null) stringResource(R.string.idle) else latestPackage!!.packageName
    val operationCardConfigs = listOf(
        OperationCardConfig(
            type = DataType.PACKAGE_APK,
            content = if (latestPackage?.apkLog.isNullOrEmpty()) stringResource(R.string.idle) else latestPackage!!.apkLog,
            state = if (latestPackage?.apkState == null) OperationState.IDLE else latestPackage!!.apkState,
            icon = ImageVector.vectorResource(R.drawable.ic_rounded_android)
        ),
        OperationCardConfig(
            type = DataType.PACKAGE_USER,
            content = if (latestPackage?.userLog.isNullOrEmpty()) stringResource(R.string.idle) else latestPackage!!.userLog,
            state = if (latestPackage?.userState == null) OperationState.IDLE else latestPackage!!.userState,
            icon = ImageVector.vectorResource(R.drawable.ic_rounded_person)
        ),
        OperationCardConfig(
            type = DataType.PACKAGE_USER_DE,
            content = if (latestPackage?.userDeLog.isNullOrEmpty()) stringResource(R.string.idle) else latestPackage!!.userDeLog,
            state = if (latestPackage?.userDeState == null) OperationState.IDLE else latestPackage!!.userDeState,
            icon = ImageVector.vectorResource(R.drawable.ic_rounded_manage_accounts)
        ),
        OperationCardConfig(
            type = DataType.PACKAGE_DATA,
            content = if (latestPackage?.dataLog.isNullOrEmpty()) stringResource(R.string.idle) else latestPackage!!.dataLog,
            state = if (latestPackage?.dataState == null) OperationState.IDLE else latestPackage!!.dataState,
            icon = ImageVector.vectorResource(R.drawable.ic_rounded_database)
        ),
        OperationCardConfig(
            type = DataType.PACKAGE_OBB,
            content = if (latestPackage?.obbLog.isNullOrEmpty()) stringResource(R.string.idle) else latestPackage!!.obbLog,
            state = if (latestPackage?.obbState == null) OperationState.IDLE else latestPackage!!.obbState,
            icon = ImageVector.vectorResource(R.drawable.ic_rounded_stadia_controller)
        ),
        OperationCardConfig(
            type = DataType.PACKAGE_MEDIA,
            content = if (latestPackage?.mediaLog.isNullOrEmpty()) stringResource(R.string.idle) else latestPackage!!.mediaLog,
            state = if (latestPackage?.mediaState == null) OperationState.IDLE else latestPackage!!.mediaState,
            icon = ImageVector.vectorResource(R.drawable.ic_rounded_image)
        )
    )

    LaunchedEffect(latestPackage) {
        withContext(Dispatchers.IO) {
            tryOn {
                // Read icon from cached internal dir.
                val bytes = File("${context.iconPath()}/${packageName}.png").readBytes()
                icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }

    LaunchedEffect(null) {
        viewModel.backupPackages {
            withContext(Dispatchers.Main) {
                navController.navigateAndPopAllStack(OperationRoutes.PackageBackupCompletion.route)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                ProcessingTopBar(scrollBehavior = scrollBehavior, title = "${stringResource(R.string.backing_up)}($operationCount/$selectedBothCount)")
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = totalProgressAnimation
                )
            }
        },
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .paddingHorizontal(CommonTokens.PaddingMedium)
                        .paddingVertical(CommonTokens.PaddingMedium)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                    ) {
                        WaverImage(size = CommonTokens.IconLargeSize, sourceBitmap = icon, progress = progressAnimation)
                        Column(modifier = Modifier.weight(1f)) {
                            TitleLargeBoldText(text = label)
                            BodySmallBoldText(text = packageName)
                        }
                    }

                    Divider()

                    operationCardConfigs.forEach { config ->
                        OperationCard(title = config.title, content = config.content, state = config.state, icon = config.icon)
                    }
                }
            }
        }
    }
}
