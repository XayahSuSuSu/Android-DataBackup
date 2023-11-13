package com.xayah.feature.task.medium.common.component

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
import com.xayah.core.database.model.MediaBackupEntity
import com.xayah.core.database.model.MediaBackupOperationEntity
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.database.model.PackageRestoreOperation
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
import com.xayah.feature.task.medium.common.R
import kotlinx.coroutines.CoroutineScope

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediumCard(
    modifier: Modifier = Modifier,
    cardSelected: Boolean,
    mediaBackup: MediaBackupEntity,
    onDataSelected: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onUpdate: suspend CoroutineScope.() -> Unit,
    chipGroup: @Composable (RowScope.() -> Unit),
) {
    LaunchedEffect(key1 = mediaBackup.sizeBytes, block = onUpdate)

    MediumCard(
        modifier = modifier,
        shimmering = false,
        label = mediaBackup.name,
        path = mediaBackup.path,
        cardSelected = cardSelected,
        dataSelected = mediaBackup.selected,
        onDataSelected = onDataSelected,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
        chipGroup = chipGroup,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediumCard(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    cardSelected: Boolean,
    packageRestore: PackageRestoreEntire,
    onDataSelected: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable (RowScope.() -> Unit),
) {
    MediumCard(
        modifier = modifier,
        enabled = enabled,
        shimmering = false,
        label = packageRestore.label,
        path = packageRestore.packageName,
        cardSelected = cardSelected,
        dataSelected = packageRestore.dataSelected,
        dataChipEnabled = packageRestore.dataExists,
        onDataSelected = onDataSelected,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
        chipGroup = chipGroup,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediumCardShimmer(
    modifier: Modifier = Modifier,
) {
    MediumCard(
        modifier = modifier,
        shimmering = true,
        label = "Shimmer",
        path = "Shimmer.Shimmer.Shimmer",
        cardSelected = false,
        dataSelected = false,
        onDataSelected = {},
        onCardClick = {},
        onCardLongClick = {},
    ) {}
}

private fun Modifier.packageCardShimmer(visible: Boolean) = shimmer(visible, 0.5f, 0.3f)

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun MediumCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shimmering: Boolean,
    label: String,
    path: String,
    cardSelected: Boolean,
    dataSelected: Boolean,
    dataChipEnabled: Boolean = true,
    onDataSelected: () -> Unit,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable (RowScope.() -> Unit),
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = enabled,
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
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(modifier = Modifier.packageCardShimmer(shimmering), text = label, fontWeight = FontWeight.Bold)
                        if (shimmering) Spacer(modifier = Modifier.size(PaddingTokens.Level1))
                        LabelSmallText(modifier = Modifier.packageCardShimmer(shimmering), text = path)
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
                        repeat(1) {
                            Spacer(
                                modifier = Modifier
                                    .paddingVertical(PaddingTokens.Level1)
                                    .size(FilterChipDefaults.Height * 2, FilterChipDefaults.Height)
                                    .packageCardShimmer(true)
                            )
                        }
                    } else {
                        OpChip(
                            enabled = enabled && dataChipEnabled,
                            selected = dataSelected,
                            label = StringResourceToken.fromStringId(R.string.data),
                            onClick = onDataSelected
                        )
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
    mediaBackupOp: MediaBackupOperationEntity,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
) {
    var msg by remember { mutableStateOf("") }

    ProcessingCard(
        modifier = modifier,
        label = mediaBackupOp.name,
        path = mediaBackupOp.path,
        showStateIcon = true,
        isProcessing = mediaBackupOp.mediaState == OperationState.PROCESSING,
        isSucceed = mediaBackupOp.isSucceed,
        msg = msg,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
    ) {
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_DATA.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(mediaBackupOp.dataOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_database),
            trailingIcon = mediaBackupOp.dataOp.state.icon,
            color = mediaBackupOp.dataOp.state.color,
            containerColor = mediaBackupOp.dataOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(mediaBackupOp.dataOp.log, "")
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    packageRestoreOp: PackageRestoreOperation,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }
    var msg by remember { mutableStateOf("") }

    LaunchedEffect(packageRestoreOp.packageName) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageRestoreOp.packageName)}")
        }
    }

    ProcessingCard(
        modifier = modifier,
        label = packageRestoreOp.label,
        path = packageRestoreOp.packageName,
        showStateIcon = true,
        isProcessing = packageRestoreOp.packageState == OperationState.PROCESSING,
        isSucceed = packageRestoreOp.isSucceed,
        msg = msg,
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
    ) {
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_APK.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(packageRestoreOp.apkOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Android),
            trailingIcon = packageRestoreOp.apkOp.state.icon,
            color = packageRestoreOp.apkOp.state.color,
            containerColor = packageRestoreOp.apkOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(packageRestoreOp.apkOp.log, "")
        }
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_USER.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(packageRestoreOp.userOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Person),
            trailingIcon = packageRestoreOp.userOp.state.icon,
            color = packageRestoreOp.userOp.state.color,
            containerColor = packageRestoreOp.userOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(packageRestoreOp.userOp.log, "")
        }
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_USER_DE.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(packageRestoreOp.userDeOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.ManageAccounts),
            trailingIcon = packageRestoreOp.userDeOp.state.icon,
            color = packageRestoreOp.userDeOp.state.color,
            containerColor = packageRestoreOp.userDeOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(packageRestoreOp.userDeOp.log, "")
        }
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_DATA.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(packageRestoreOp.dataOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_database),
            trailingIcon = packageRestoreOp.dataOp.state.icon,
            color = packageRestoreOp.dataOp.state.color,
            containerColor = packageRestoreOp.dataOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(packageRestoreOp.dataOp.log, "")
        }
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_OBB.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(packageRestoreOp.obbOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_stadia_controller),
            trailingIcon = packageRestoreOp.obbOp.state.icon,
            color = packageRestoreOp.obbOp.state.color,
            containerColor = packageRestoreOp.obbOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(packageRestoreOp.obbOp.log, "")
        }
        AssistChip(
            enabled = true,
            title = StringResourceToken.fromString(DataType.PACKAGE_MEDIA.type.uppercase()),
            subtitle = StringResourceToken.fromString(formatSize(packageRestoreOp.mediaOp.bytes.toDouble())),
            leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Image),
            trailingIcon = packageRestoreOp.mediaOp.state.icon,
            color = packageRestoreOp.mediaOp.state.color,
            containerColor = packageRestoreOp.mediaOp.state.containerColor,
        ) {
            msg = msg.ifNotTheSame(packageRestoreOp.mediaOp.log, "")
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    mediaBackup: MediaBackupEntity,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
) {
    ProcessingCard(
        modifier = modifier,
        label = mediaBackup.name,
        path = mediaBackup.path,
        showStateIcon = false,
        isProcessing = false,
        isSucceed = false,
        msg = "",
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
    ) {
        RoundChip(text = DataType.MEDIA_MEDIA.type.uppercase())
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    packageRestore: PackageRestoreEntire,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(packageRestore.packageName) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageRestore.packageName)}")
        }
    }

    ProcessingCard(
        modifier = modifier,
        label = packageRestore.label,
        path = packageRestore.packageName,
        showStateIcon = false,
        isProcessing = false,
        isSucceed = false,
        msg = "",
        onCardClick = onCardClick,
        onCardLongClick = onCardLongClick,
    ) {
        if (packageRestore.apkSelected) RoundChip(text = DataType.PACKAGE_APK.type.uppercase())
        if (packageRestore.dataSelected) {
            RoundChip(text = DataType.PACKAGE_USER.type.uppercase())
            RoundChip(text = DataType.PACKAGE_USER_DE.type.uppercase())
            RoundChip(text = DataType.PACKAGE_DATA.type.uppercase())
            RoundChip(text = DataType.PACKAGE_OBB.type.uppercase())
            RoundChip(text = DataType.PACKAGE_MEDIA.type.uppercase())
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    label: String,
    path: String,
    showStateIcon: Boolean,
    isProcessing: Boolean,
    isSucceed: Boolean,
    msg: String,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    chipGroup: @Composable (RowScope.() -> Unit),
) {
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
                    Column(modifier = Modifier.weight(1f)) {
                        TitleMediumText(text = label, fontWeight = FontWeight.Bold)
                        LabelSmallText(text = path)
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
