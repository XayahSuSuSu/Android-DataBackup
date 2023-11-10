package com.xayah.feature.task.packages.common.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.database.model.formatSize
import com.xayah.core.model.DataType
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.AnimatedMultiColorLinearProgressIndicator
import com.xayah.core.ui.component.AssistChip
import com.xayah.core.ui.component.Card
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.ignorePaddingHorizontal
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.component.shimmer
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.material3.VerticalDivider
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.MultiColorProgress
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.color
import com.xayah.core.ui.util.containerColor
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.icon
import com.xayah.core.ui.util.value
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import com.xayah.core.util.ifNotTheSame
import com.xayah.core.util.withIOContext
import com.xayah.feature.task.packages.common.R

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PackageCard(
    modifier: Modifier = Modifier,
    cardSelected: Boolean,
    packageBackup: PackageBackupEntire,
    onApkSelected: () -> Unit,
    onDataSelected: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(packageBackup.packageName) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageBackup.packageName)}")
        }
    }

    PackageCard(
        modifier = modifier,
        shimmering = false,
        label = packageBackup.label,
        packageName = packageBackup.packageName,
        icon = icon,
        cardSelected = cardSelected,
        apkSelected = packageBackup.apkSelected,
        dataSelected = packageBackup.dataSelected,
        onApkSelected = onApkSelected,
        onDataSelected = onDataSelected,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
        chipGroup = chipGroup,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PackageCardShimmer(
    modifier: Modifier = Modifier,
) {
    PackageCard(
        modifier = modifier,
        shimmering = true,
        label = "Shimmer",
        packageName = "Shimmer.Shimmer.Shimmer",
        icon = null,
        cardSelected = false,
        apkSelected = false,
        dataSelected = false,
        onApkSelected = {},
        onDataSelected = {},
        onCardClick = {},
        onCardLongClick = {},
        chipGroup = {},
    )
}

private fun Modifier.packageCardShimmer(visible: Boolean) = shimmer(visible, 0.5f, 0.3f)

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PackageCard(
    modifier: Modifier = Modifier,
    shimmering: Boolean,
    label: String,
    packageName: String,
    icon: Any?,
    cardSelected: Boolean,
    apkSelected: Boolean,
    dataSelected: Boolean,
    onApkSelected: () -> Unit,
    onDataSelected: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCardLongClick()
        },
        border = if (cardSelected) outlinedCardBorder(borderColor = ColorSchemeKeyTokens.Primary.toColor()) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paddingTop(PaddingTokens.Level3)
                .paddingHorizontal(PaddingTokens.Level3)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(CardTokens.IconSize)
                            .packageCardShimmer(shimmering),
                        model = ImageRequest.Builder(context)
                            .data(icon)
                            .crossfade(true)
                            .build(),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(modifier = Modifier.packageCardShimmer(shimmering), text = label, fontWeight = FontWeight.Bold)
                        if (shimmering) Spacer(modifier = Modifier.size(PaddingTokens.Level1))
                        LabelSmallText(modifier = Modifier.packageCardShimmer(shimmering), text = packageName)
                    }
                    if (cardSelected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorSchemeKeyTokens.Primary.toColor(),
                    )
                }
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(PaddingTokens.Level3)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(PaddingTokens.Level3),
                        horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                        content = {
                            chipGroup()
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(PaddingTokens.Level3)
                        .fillMaxWidth()
                        .background(color = ColorSchemeKeyTokens.InverseOnSurface.toColor()),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3, Alignment.End)
                ) {
                    if (shimmering) {
                        repeat(2) {
                            Spacer(
                                modifier = Modifier
                                    .paddingVertical(PaddingTokens.Level1)
                                    .size(FilterChipDefaults.Height * 2, FilterChipDefaults.Height)
                                    .packageCardShimmer(true)
                            )
                        }
                    } else {
                        OpChip(selected = apkSelected, label = StringResourceToken.fromStringId(R.string.apk), onClick = onApkSelected)
                        OpChip(selected = dataSelected, label = StringResourceToken.fromStringId(R.string.data), onClick = onDataSelected)
                    }

                    Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ProcessingInfoCard(
    modifier: Modifier = Modifier,
    targetPath: String,
    availableBytes: Double,
    rawBytes: Double,
    totalBytes: Double,
    remainingCount: Int,
    succeedCount: Int,
    failedCount: Int,
    timer: String,
) {
    Card(
        modifier = modifier,
        onClick = {},
        onLongClick = {}
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level3), verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level1)) {
            Row(horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(CardTokens.IconSize),
                    imageVector = Icons.Rounded.DataUsage,
                    tint = ColorSchemeKeyTokens.Primary.toColor(),
                    contentDescription = null
                )
                Column {
                    LabelLargeText(
                        text = targetPath,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        fontWeight = FontWeight.Bold
                    )
                    LabelSmallText(
                        text = "${formatSize(totalBytes - availableBytes)} (+${formatSize(rawBytes)}) / ${formatSize(totalBytes)}",
                        color = ColorSchemeKeyTokens.Secondary.toColor(),
                    )
                }
            }

            AnimatedMultiColorLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
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
                trackColor = ColorSchemeKeyTokens.InverseOnSurface.toColor(),
                strokeCap = StrokeCap.Round,
            )

            Row(
                modifier = Modifier
                    .ignorePaddingHorizontal(PaddingTokens.Level3)
                    .fillMaxWidth()
                    .paddingTop(PaddingTokens.Level1)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.remain).value,
                        color = ColorSchemeKeyTokens.Tertiary.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    TitleLargeText(
                        text = remainingCount.toString(),
                        color = ColorSchemeKeyTokens.Tertiary.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                VerticalDivider(modifier = Modifier.paddingVertical(4.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.succeed).value,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    TitleLargeText(
                        text = succeedCount.toString(),
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                VerticalDivider(modifier = Modifier.paddingVertical(4.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.failed).value,
                        color = ColorSchemeKeyTokens.Error.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    TitleLargeText(
                        text = failedCount.toString(),
                        color = ColorSchemeKeyTokens.Error.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                VerticalDivider(modifier = Modifier.paddingVertical(4.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    LabelSmallText(
                        text = StringResourceToken.fromStringId(R.string.time).value,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                    )
                    TitleLargeText(
                        text = timer,
                        color = ColorSchemeKeyTokens.Primary.toColor(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    packageBackupOp: PackageBackupOperation,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }
    var msg by remember { mutableStateOf("") }

    LaunchedEffect(packageBackupOp.packageName) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageBackupOp.packageName)}")
        }
    }

    ProcessingCard(
        modifier = modifier,
        label = packageBackupOp.label,
        packageName = packageBackupOp.packageName,
        icon = icon,
        showStateIcon = true,
        isProcessing = packageBackupOp.packageState == OperationState.PROCESSING,
        isSucceed = packageBackupOp.isSucceed,
        msg = msg,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
        chipGroup = {
            AssistChip(
                enabled = true,
                title = StringResourceToken.fromString(DataType.PACKAGE_APK.type.uppercase()),
                subtitle = StringResourceToken.fromString(formatSize(packageBackupOp.apkOp.bytes.toDouble())),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Android),
                trailingIcon = packageBackupOp.apkOp.state.icon,
                color = packageBackupOp.apkOp.state.color,
                containerColor = packageBackupOp.apkOp.state.containerColor,
            ) {
                msg = msg.ifNotTheSame(packageBackupOp.apkOp.log, "")
            }
            AssistChip(
                enabled = true,
                title = StringResourceToken.fromString(DataType.PACKAGE_USER.type.uppercase()),
                subtitle = StringResourceToken.fromString(formatSize(packageBackupOp.userOp.bytes.toDouble())),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Person),
                trailingIcon = packageBackupOp.userOp.state.icon,
                color = packageBackupOp.userOp.state.color,
                containerColor = packageBackupOp.userOp.state.containerColor,
            ) {
                msg = msg.ifNotTheSame(packageBackupOp.userOp.log, "")
            }
            AssistChip(
                enabled = true,
                title = StringResourceToken.fromString(DataType.PACKAGE_USER_DE.type.uppercase()),
                subtitle = StringResourceToken.fromString(formatSize(packageBackupOp.userDeOp.bytes.toDouble())),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.ManageAccounts),
                trailingIcon = packageBackupOp.userDeOp.state.icon,
                color = packageBackupOp.userDeOp.state.color,
                containerColor = packageBackupOp.userDeOp.state.containerColor,
            ) {
                msg = msg.ifNotTheSame(packageBackupOp.userDeOp.log, "")
            }
            AssistChip(
                enabled = true,
                title = StringResourceToken.fromString(DataType.PACKAGE_DATA.type.uppercase()),
                subtitle = StringResourceToken.fromString(formatSize(packageBackupOp.dataOp.bytes.toDouble())),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_database),
                trailingIcon = packageBackupOp.dataOp.state.icon,
                color = packageBackupOp.dataOp.state.color,
                containerColor = packageBackupOp.dataOp.state.containerColor,
            ) {
                msg = msg.ifNotTheSame(packageBackupOp.dataOp.log, "")
            }
            AssistChip(
                enabled = true,
                title = StringResourceToken.fromString(DataType.PACKAGE_OBB.type.uppercase()),
                subtitle = StringResourceToken.fromString(formatSize(packageBackupOp.obbOp.bytes.toDouble())),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_stadia_controller),
                trailingIcon = packageBackupOp.obbOp.state.icon,
                color = packageBackupOp.obbOp.state.color,
                containerColor = packageBackupOp.obbOp.state.containerColor,
            ) {
                msg = msg.ifNotTheSame(packageBackupOp.obbOp.log, "")
            }
            AssistChip(
                enabled = true,
                title = StringResourceToken.fromString(DataType.PACKAGE_MEDIA.type.uppercase()),
                subtitle = StringResourceToken.fromString(formatSize(packageBackupOp.mediaOp.bytes.toDouble())),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Image),
                trailingIcon = packageBackupOp.mediaOp.state.icon,
                color = packageBackupOp.mediaOp.state.color,
                containerColor = packageBackupOp.mediaOp.state.containerColor,
            ) {
                msg = msg.ifNotTheSame(packageBackupOp.mediaOp.log, "")
            }
        },
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    packageBackup: PackageBackupEntire,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(packageBackup.packageName) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageBackup.packageName)}")
        }
    }

    ProcessingCard(
        modifier = modifier,
        label = packageBackup.label,
        packageName = packageBackup.packageName,
        icon = icon,
        showStateIcon = false,
        isProcessing = false,
        isSucceed = false,
        msg = "",
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
        chipGroup = {
            if (packageBackup.apkSelected) RoundChip(text = DataType.PACKAGE_APK.type.uppercase())
            if (packageBackup.dataSelected) {
                RoundChip(text = DataType.PACKAGE_USER.type.uppercase())
                RoundChip(text = DataType.PACKAGE_USER_DE.type.uppercase())
                RoundChip(text = DataType.PACKAGE_DATA.type.uppercase())
                RoundChip(text = DataType.PACKAGE_OBB.type.uppercase())
                RoundChip(text = DataType.PACKAGE_MEDIA.type.uppercase())
            }
        },
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    label: String,
    packageName: String,
    icon: Any?,
    showStateIcon: Boolean,
    isProcessing: Boolean,
    isSucceed: Boolean,
    msg: String,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
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
                .paddingTop(PaddingTokens.Level3)
                .paddingHorizontal(PaddingTokens.Level3)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1)
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(CardTokens.IconSize),
                        model = ImageRequest.Builder(context)
                            .data(icon)
                            .crossfade(true)
                            .build(),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(text = label, fontWeight = FontWeight.Bold)
                        LabelSmallText(text = packageName)
                    }

                    if (showStateIcon) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .size(24.dp), strokeCap = StrokeCap.Round
                            )
                        } else {
                            if (isSucceed)
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.Top),
                                    tint = ColorSchemeKeyTokens.Primary.toColor(),
                                )
                            else
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.Top),
                                    tint = ColorSchemeKeyTokens.Error.toColor(),
                                )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(PaddingTokens.Level3)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(PaddingTokens.Level3),
                        horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                        content = {
                            chipGroup()
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(PaddingTokens.Level3)
                        .animateContentSize()
                        .fillMaxWidth()
                        .background(color = ColorSchemeKeyTokens.InverseOnSurface.toColor()),
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                ) {
                    if (msg.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))

                        LabelSmallText(modifier = Modifier.paddingVertical(PaddingTokens.Level1), text = msg)

                        Spacer(modifier = Modifier.size(PaddingTokens.Level0))
                    }
                }
            }
        }
    }
}
