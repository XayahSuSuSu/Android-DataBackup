package com.xayah.feature.main.task

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xayah.core.data.util.typeIconToken
import com.xayah.core.data.util.typeNameToken
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.AnimatedMultiColorLinearProgressIndicator
import com.xayah.core.ui.component.AutoTitleLargeText
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.DataChip
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.ignorePaddingHorizontal
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.material3.VerticalDivider
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.MultiColorProgress
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.color
import com.xayah.core.ui.util.containerColor
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.icon
import com.xayah.core.ui.util.value
import com.xayah.core.util.DateUtil
import com.xayah.core.util.ifNotTheSame

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    t: TaskEntity,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        enabled = true,
        onClick = onClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            expanded = true
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingTokens.Level4)
        ) {
            Column {
                TitleMediumText(
                    text = StringResourceToken.fromStringArgs(
                        StringResourceToken.fromStringId(R.string.task),
                        StringResourceToken.fromString(t.id.toString()),
                    ).value, fontWeight = FontWeight.Bold
                )
                LabelSmallText(text = DateUtil.formatTimestamp(t.startTimestamp))
                LabelSmallText(text = t.backupDir)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingTop(PaddingTokens.Level2),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                    content = {
                        RoundChip(
                            text = when (t.opType) {
                                OpType.BACKUP -> StringResourceToken.fromStringId(R.string.backup).value
                                OpType.RESTORE -> StringResourceToken.fromStringId(R.string.restore).value
                            }
                        )
                        RoundChip(
                            text = when (t.taskType) {
                                TaskType.PACKAGE -> StringResourceToken.fromStringId(R.string.app_and_data).value
                                TaskType.MEDIA -> StringResourceToken.fromStringId(R.string.media).value
                            }
                        )
                        RoundChip(
                            text = if (t.cloud.isEmpty()) StringResourceToken.fromStringId(R.string.local).value
                            else StringResourceToken.fromStringId(R.string.cloud).value
                        )
                        RoundChip(
                            text = StringResourceToken.fromStringArgs(
                                StringResourceToken.fromString("${t.successCount} "),
                                StringResourceToken.fromStringId(R.string.succeed)
                            ).value
                        )
                        RoundChip(
                            text = StringResourceToken.fromStringArgs(
                                StringResourceToken.fromString("${t.failureCount} "),
                                StringResourceToken.fromStringId(R.string.failed)
                            ).value
                        )
                    }
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun TaskInfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVectorToken,
    title: String,
    subtitle: String,
    multiColorProgress: List<MultiColorProgress>,
    remainingCount: Int,
    successCount: Int,
    failureCount: Int,
    timer: String,
) {
    Card(
        modifier = modifier,
        onClick = {},
        onLongClick = {}
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level4), verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(SizeTokens.Level36),
                    imageVector = icon.value,
                    tint = ColorSchemeKeyTokens.Primary.toColor(),
                    contentDescription = null
                )
                Column {
                    LabelLargeText(
                        text = title,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        fontWeight = FontWeight.Bold
                    )
                    LabelSmallText(
                        text = subtitle,
                        color = ColorSchemeKeyTokens.Secondary.toColor(),
                    )
                }
            }

            if (multiColorProgress.isNotEmpty()) {
                AnimatedMultiColorLinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    multiColorProgress = multiColorProgress,
                    trackColor = ColorSchemeKeyTokens.InverseOnSurface.toColor(),
                    strokeCap = StrokeCap.Round,
                )
            } else {
                Divider(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier
                    .ignorePaddingHorizontal(PaddingTokens.Level4)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.remain).value,
                        color = ColorSchemeKeyTokens.Tertiary.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    AutoTitleLargeText(
                        modifier = modifier.paddingHorizontal(PaddingTokens.Level2),
                        text = remainingCount.toString(),
                        color = ColorSchemeKeyTokens.Tertiary.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                }
                VerticalDivider(modifier = Modifier.paddingVertical(PaddingTokens.Level1))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.succeed).value,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    AutoTitleLargeText(
                        modifier = modifier.paddingHorizontal(PaddingTokens.Level2),
                        text = successCount.toString(),
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                }
                VerticalDivider(modifier = Modifier.paddingVertical(PaddingTokens.Level1))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.failed).value,
                        color = ColorSchemeKeyTokens.Error.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    AutoTitleLargeText(
                        modifier = modifier.paddingHorizontal(PaddingTokens.Level2),
                        text = failureCount.toString(),
                        color = ColorSchemeKeyTokens.Error.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                }
                VerticalDivider(modifier = Modifier.paddingVertical(PaddingTokens.Level1))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.time).value,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    AutoTitleLargeText(
                        modifier = modifier.paddingHorizontal(PaddingTokens.Level2),
                        text = timer,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun TaskItemCard(
    modifier: Modifier = Modifier,
    label: String,
    packageName: String,
    isProcessing: Boolean,
    isSuccess: Boolean,
    msg: String,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    TaskItemCard(
        modifier = modifier,
        title = label,
        subtitle = packageName,
        icon = {
            PackageIconImage(packageName = packageName, size = SizeTokens.Level36)
        },
        isProcessing = isProcessing,
        isSuccess = isSuccess,
        msg = msg,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
        chipGroup = chipGroup
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun TaskItemCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: @Composable (() -> Unit)? = null,
    isProcessing: Boolean,
    isSuccess: Boolean,
    msg: String,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier,
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCardLongClick()
        },
        border = outlinedCardBorder(borderColor = ColorSchemeKeyTokens.Primary.toColor()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paddingTop(PaddingTokens.Level4)
                .paddingHorizontal(PaddingTokens.Level4)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)
                ) {
                    icon?.invoke()
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(text = title, fontWeight = FontWeight.Bold)
                        LabelSmallText(text = subtitle)
                    }

                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Top)
                                .size(24.dp),
                            strokeCap = StrokeCap.Round
                        )
                    } else {
                        Icon(
                            imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Top),
                            tint = ColorSchemeKeyTokens.Primary.toColor(),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(PaddingTokens.Level4)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(PaddingTokens.Level4),
                        horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                        content = {
                            chipGroup()
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(PaddingTokens.Level4)
                        .animateContentSize()
                        .fillMaxWidth()
                        .background(color = ColorSchemeKeyTokens.InverseOnSurface.toColor()),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                ) {
                    if (msg.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                        LabelSmallText(modifier = Modifier.paddingVertical(PaddingTokens.Level2), text = msg)

                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                    }
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun TaskPackageItemCard(
    modifier: Modifier = Modifier,
    item: TaskDetailPackageEntity,
) {
    var msg by remember { mutableStateOf("") }

    TaskItemCard(
        modifier = modifier,
        label = item.packageEntity.packageInfo.label,
        packageName = item.packageEntity.packageName,
        isProcessing = item.isFinished.not(),
        isSuccess = item.isSuccess,
        msg = msg,
        onCardClick = {},
        onCardLongClick = {}
    ) {
        val apkType = DataType.PACKAGE_APK
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = apkType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.apkInfo.bytes.toDouble().formatSize()),
            leadingIcon = apkType.typeIconToken,
            trailingIcon = item.apkInfo.state.icon,
            color = item.apkInfo.state.color,
            containerColor = item.apkInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.apkInfo.log, "")
        }

        val userType = DataType.PACKAGE_USER
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = userType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.userInfo.bytes.toDouble().formatSize()),
            leadingIcon = userType.typeIconToken,
            trailingIcon = item.userInfo.state.icon,
            color = item.userInfo.state.color,
            containerColor = item.userInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.userInfo.log, "")
        }

        val userDeType = DataType.PACKAGE_USER_DE
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = userDeType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.userDeInfo.bytes.toDouble().formatSize()),
            leadingIcon = userDeType.typeIconToken,
            trailingIcon = item.userDeInfo.state.icon,
            color = item.userDeInfo.state.color,
            containerColor = item.userDeInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.userDeInfo.log, "")
        }

        val dataType = DataType.PACKAGE_DATA
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = dataType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.dataInfo.bytes.toDouble().formatSize()),
            leadingIcon = dataType.typeIconToken,
            trailingIcon = item.dataInfo.state.icon,
            color = item.dataInfo.state.color,
            containerColor = item.dataInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.dataInfo.log, "")
        }

        val obbType = DataType.PACKAGE_OBB
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = obbType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.obbInfo.bytes.toDouble().formatSize()),
            leadingIcon = obbType.typeIconToken,
            trailingIcon = item.obbInfo.state.icon,
            color = item.obbInfo.state.color,
            containerColor = item.obbInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.obbInfo.log, "")
        }

        val mediaType = DataType.PACKAGE_MEDIA
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = mediaType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.mediaInfo.bytes.toDouble().formatSize()),
            leadingIcon = mediaType.typeIconToken,
            trailingIcon = item.mediaInfo.state.icon,
            color = item.mediaInfo.state.color,
            containerColor = item.mediaInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.mediaInfo.log, "")
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun TaskMediaItemCard(
    modifier: Modifier = Modifier,
    item: TaskDetailMediaEntity,
) {
    var msg by remember { mutableStateOf("") }

    TaskItemCard(
        modifier = modifier,
        title = item.mediaEntity.name,
        subtitle = item.mediaEntity.path,
        isProcessing = item.isFinished.not(),
        isSuccess = item.isSuccess,
        msg = msg,
        onCardClick = {},
        onCardLongClick = {}
    ) {
        val dataType = DataType.PACKAGE_DATA
        DataChip(
            modifier = Modifier.wrapContentSize(),
            enabled = true,
            title = dataType.typeNameToken,
            subtitle = StringResourceToken.fromString(item.dataInfo.bytes.toDouble().formatSize()),
            leadingIcon = dataType.typeIconToken,
            trailingIcon = item.dataInfo.state.icon,
            color = item.dataInfo.state.color,
            containerColor = item.dataInfo.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(item.dataInfo.log, "")
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DetailScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState,
    taskId: Long,
    taskState: TaskEntity?,
    internalTimerState: String,
    taskTimerState: String,
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = StringResourceToken.fromStringArgs(
                    StringResourceToken.fromStringId(R.string.task),
                    StringResourceToken.fromString(taskId.toString())
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .paddingHorizontal(PaddingTokens.Level4),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(PaddingTokens.Level4))
                        taskState?.apply {
                            TaskInfoCard(
                                icon = ImageVectorToken.fromVector(Icons.Rounded.PhoneAndroid),
                                title = StringResourceToken.fromStringId(R.string.internal_storage).value,
                                subtitle = "${(totalBytes - availableBytes).formatSize()} (+${rawBytes.formatSize()}) / ${totalBytes.formatSize()}",
                                multiColorProgress = listOf(
                                    MultiColorProgress(
                                        progress = ((totalBytes - availableBytes) / totalBytes).toFloat().takeIf { it.isNaN().not() } ?: 0f,
                                        color = ColorSchemeKeyTokens.Primary.toColor()
                                    ),
                                    MultiColorProgress(
                                        progress = (rawBytes / totalBytes).toFloat().takeIf { it.isNaN().not() } ?: 0f,
                                        color = ColorSchemeKeyTokens.Error.toColor()
                                    ),
                                ),
                                remainingCount = totalCount - successCount - failureCount,
                                successCount = successCount,
                                failureCount = failureCount,
                                timer = if (isProcessing) internalTimerState else taskTimerState
                            )
                        }
                    }

                    content()

                    item {
                        Spacer(modifier = Modifier.paddingBottom(PaddingTokens.Level4))
                    }
                }
            }
        }
    }
}