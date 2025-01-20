package com.xayah.feature.main.history

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.DataType
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.util.get
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.StateView
import com.xayah.core.util.maybePopBackStack

@Composable
fun TaskDetailsRoute(
    viewModel: TaskDetailsViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: TaskDetailsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskDetailsScreen(uiState)
    LaunchedEffect(uiState) {
        if (uiState is TaskDetailsUiState.Error) {
            navController.maybePopBackStack()
        }
    }
}

@SuppressLint("StringFormatInvalid")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDetailsScreen(uiState: TaskDetailsUiState) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.task_details),
            )
        }
    ) { innerPadding ->
        AnimatedContent(uiState, label = AnimationTokens.AnimatedContentLabel) { state ->
            when (state) {
                is TaskDetailsUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            InnerTopSpacer(innerPadding = innerPadding)
                        }

                        item {
                            Title(title = stringResource(R.string.preprocessing)) {
                                state.preprocessingInfoList.forEach {
                                    it.ProcessingItem()
                                }
                            }
                        }

                        items(items = state.appInfoList) {
                            Title(title = it.packageEntity.packageInfo.label.ifEmpty { it.packageEntity.packageName }) {
                                it.AppDataItem(DataType.PACKAGE_APK)
                                it.AppDataItem(DataType.PACKAGE_USER)
                                it.AppDataItem(DataType.PACKAGE_USER_DE)
                                it.AppDataItem(DataType.PACKAGE_DATA)
                                it.AppDataItem(DataType.PACKAGE_OBB)
                                it.AppDataItem(DataType.PACKAGE_MEDIA)
                            }
                        }

                        items(items = state.fileInfoList) {
                            Title(title = it.mediaEntity.name.ifEmpty { it.mediaEntity.path }) {
                                it.FileDataItem(DataType.MEDIA_MEDIA)
                            }
                        }

                        item {
                            Title(title = stringResource(R.string.post_processing)) {
                                state.postProcessingInfoList.forEach {
                                    it.ProcessingItem()
                                }
                            }
                        }

                        item {
                            InnerBottomSpacer(innerPadding = innerPadding)
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun TaskDetailPackageEntity.AppDataItem(dataType: DataType) {
    val info by remember(this, dataType) {
        mutableStateOf(get(dataType))
    }
    Clickable(
        title = dataType.type.uppercase(),
        value = info.log.ifEmpty { null },
        leadingIcon = {
            PackageIconImage(packageName = packageEntity.packageName, size = SizeTokens.Level32)
        },
        trailingIcon = {
            info.state.StateView()
        }
    ) {
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun TaskDetailMediaEntity.FileDataItem(dataType: DataType) {
    Clickable(
        title = dataType.type.uppercase(),
        value = mediaInfo.log.ifEmpty { null },
        leadingIcon = {
            PackageIconImage(icon = Icons.Rounded.Folder, packageName = "", inCircleShape = true, size = SizeTokens.Level32)
        },
        trailingIcon = {
            mediaInfo.state.StateView()
        }
    ) {
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun ProcessingInfoEntity.ProcessingItem() {
    Clickable(
        title = title,
        value = log.ifEmpty { null },
        leadingIcon = {
            PackageIconImage(icon = ImageVector.vectorResource(com.xayah.core.ui.R.drawable.ic_rounded_hourglass_empty), packageName = "", inCircleShape = true, size = SizeTokens.Level32)
        },
        trailingIcon = {
            state.StateView()
        }
    ) {
    }
}
