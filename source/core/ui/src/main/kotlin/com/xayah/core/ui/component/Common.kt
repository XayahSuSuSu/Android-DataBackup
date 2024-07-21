package com.xayah.core.ui.component

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.R
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.joinOf
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import com.xayah.core.util.withIOContext
import kotlin.math.min

@ExperimentalMaterial3Api
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.paddingHorizontal(SizeTokens.Level16), verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)) {
        LabelLargeText(text = title, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value, fontWeight = FontWeight.SemiBold)

        content()
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    colorContainer: ThemedColorSchemeKeyTokens,
    colorL80D20: ThemedColorSchemeKeyTokens,
    onColorContainer: ThemedColorSchemeKeyTokens,
    trailingIcon: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(SizeTokens.Level18),
        colors = CardDefaults.cardColors(containerColor = colorContainer.value.withState(enabled)),
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .padding(SizeTokens.Level12)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level10)
        ) {
            Surface(
                modifier = Modifier.size(SizeTokens.Level36),
                shape = CircleShape,
                color = colorL80D20.value.withState(enabled),
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ) {
                Icon(
                    modifier = Modifier.padding(SizeTokens.Level8),
                    imageVector = icon,
                    tint = onColorContainer.value.withState(enabled),
                    contentDescription = null,
                )
            }
            content()
            trailingIcon?.invoke(this)
        }
    }
}


@ExperimentalFoundationApi
@Composable
fun PackageIcons(
    modifier: Modifier,
    packages: List<PackageEntity>,
    maxDisplayNum: Int = 6,
    size: Dp = SizeTokens.Level24,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    var icons by remember { mutableStateOf(listOf<Drawable?>()) }
    LaunchedEffect(packages) {
        // Read icon from cached internal dir.
        withIOContext {
            val tmp = mutableListOf<Drawable?>()
            for (i in 0 until min(maxDisplayNum, packages.size)) {
                tmp.add(BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packages[i].packageName)}"))
            }
            icons = tmp
        }
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(-SizeTokens.Level4)) {
        for ((index, icon) in icons.withIndex()) {
            if (index == icons.size - 1) break
            if (icon == null) {
                Surface(
                    modifier = Modifier.size(size),
                    shape = ClippedCircleShape,
                    color = ThemedColorSchemeKeyTokens.PrimaryContainer.value,
                    onClick = onClick,
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LabelMediumText(text = "${packages[index].packageInfo.label.firstOrNull() ?: ""}", color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value)
                    }
                }
            } else {
                AsyncImage(
                    modifier = Modifier
                        .size(size)
                        .clip(ClippedCircleShape),
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
            }
        }

        if (packages.size <= maxDisplayNum && icons.isNotEmpty()) {
            val last = icons.last()
            if (last == null) {
                Surface(
                    modifier = Modifier.size(size),
                    shape = CircleShape,
                    color = ThemedColorSchemeKeyTokens.PrimaryContainer.value,
                    onClick = onClick,
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LabelMediumText(text = "${packages[icons.lastIndex].packageInfo.label.firstOrNull() ?: ""}", color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value)
                    }
                }
            } else {
                AsyncImage(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    model = ImageRequest.Builder(context)
                        .data(last)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )
            }
        } else if (packages.size - maxDisplayNum > 0) {
            Surface(
                modifier = Modifier.size(size),
                shape = CircleShape,
                color = ThemedColorSchemeKeyTokens.PrimaryContainer.value,
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LabelMediumText(text = "+${packages.size - maxDisplayNum + 1}", color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value)
                }
            }
        }
    }
}


@ExperimentalMaterial3Api
@ExperimentalLayoutApi
@ExperimentalFoundationApi
@Composable
fun PackageItem(item: PackageEntity, checked: Boolean? = null, onCheckedChange: ((Boolean) -> Unit)?, filterMode: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    com.xayah.core.ui.material3.Surface(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .paddingTop(SizeTokens.Level16)
                    .paddingHorizontal(SizeTokens.Level16)
                    .then(if (filterMode.not()) Modifier.paddingBottom(SizeTokens.Level16) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
            ) {
                PackageIconImage(packageName = item.packageName, size = SizeTokens.Level32)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level4)) {
                        RoundChip {
                            LabelSmallText(
                                modifier = Modifier.paddingHorizontal(SizeTokens.Level8),
                                text = "${item.userId}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TitleLargeText(
                            text = item.packageInfo.label.ifEmpty { stringResource(id = R.string.unknown) },
                            color = (if (item.preserveId != 0L) ThemedColorSchemeKeyTokens.YellowPrimary else ThemedColorSchemeKeyTokens.OnSurface).value,
                            maxLines = 1,
                        )
                    }
                    BodyMediumText(
                        text = item.packageName,
                        color = ThemedColorSchemeKeyTokens.Outline.value,
                        maxLines = 1,
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(SizeTokens.Level36)
                        .width(SizeTokens.Level1)
                        .fillMaxHeight()
                )
                CheckIconButton(checked = checked ?: item.extraInfo.activated, onCheckedChange = onCheckedChange)
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingStart(SizeTokens.Level64)
                    .paddingEnd(SizeTokens.Level64)
                    .paddingBottom(SizeTokens.Level16),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8),
                content = {
                    val ssaid = item.extraInfo.ssaid
                    val hasKeystore = item.extraInfo.hasKeystore
                    val storageStatsFormat = when (item.indexInfo.opType) {
                        OpType.BACKUP -> item.storageStatsBytes
                        OpType.RESTORE -> item.displayStatsBytes
                    }

                    if (item.apkSelected) {
                        AssistChip(
                            enabled = true,
                            label = stringResource(id = R.string.apk),
                            leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_android),
                            trailingIcon = null,
                            color = ThemedColorSchemeKeyTokens.RedPrimary,
                            containerColor = ThemedColorSchemeKeyTokens.RedPrimaryContainer,
                            border = null,
                        )
                    }

                    if (item.userSelected || item.userDeSelected || item.dataSelected || item.obbSelected || item.mediaSelected) {
                        AssistChip(
                            enabled = true,
                            label = joinOf(stringResource(id = R.string.data), " (${item.dataSelectedCount})"),
                            leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_database),
                            trailingIcon = null,
                            color = ThemedColorSchemeKeyTokens.RedPrimary,
                            containerColor = ThemedColorSchemeKeyTokens.RedPrimaryContainer,
                            border = null,
                        )
                    }

                    if (item.preserveId != 0L) {
                        AssistChip(
                            enabled = true,
                            label = stringResource(id = R.string._protected),
                            leadingIcon = Icons.Outlined.Shield,
                            trailingIcon = null,
                            color = ThemedColorSchemeKeyTokens.YellowPrimary,
                            containerColor = ThemedColorSchemeKeyTokens.YellowPrimaryContainer,
                            border = null,
                        )
                    }
                    if (storageStatsFormat != (0).toDouble()) {
                        AssistChip(
                            enabled = true,
                            label = storageStatsFormat.formatSize(),
                            leadingIcon = Icons.Outlined.Folder,
                            trailingIcon = null,
                            color = ThemedColorSchemeKeyTokens.Primary,
                            containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
                            border = null,
                        )
                    }
                    if (ssaid.isNotEmpty()) AssistChip(
                        enabled = true,
                        label = stringResource(id = R.string.ssaid),
                        leadingIcon = Icons.Outlined.Pin,
                        trailingIcon = null,
                        color = ThemedColorSchemeKeyTokens.Primary,
                        containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
                        border = null,
                    )
                    if (hasKeystore) AssistChip(
                        enabled = true,
                        label = stringResource(id = R.string.keystore),
                        leadingIcon = Icons.Outlined.Key,
                        trailingIcon = null,
                        color = ThemedColorSchemeKeyTokens.Primary,
                        containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
                        border = null,
                    )
                }
            )
        }
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@Composable
fun MediaItem(item: MediaEntity, enabled: Boolean? = null, checked: Boolean? = null, onCheckedChange: ((Boolean) -> Unit)?, filterMode: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val existed = item.existed
    val _enabled = enabled ?: item.enabled
    com.xayah.core.ui.material3.Surface(onClick = onClick) {
        Column {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .paddingTop(SizeTokens.Level16)
                    .paddingHorizontal(SizeTokens.Level16)
                    .then(if (filterMode.not()) Modifier.paddingBottom(SizeTokens.Level16) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
            ) {
                MediaIconImage(name = item.name.firstOrNull()?.toString() ?: "", size = SizeTokens.Level32)
                Column(modifier = Modifier.weight(1f)) {
                    TitleLargeText(
                        text = item.name.ifEmpty { stringResource(id = R.string.unknown) },
                        color = (if (item.preserveId != 0L) ThemedColorSchemeKeyTokens.YellowPrimary else ThemedColorSchemeKeyTokens.OnSurface).value
                    )
                    BodyMediumText(
                        text = item.path.ifEmpty { stringResource(id = R.string.specify_a_path) },
                        color = (if (item.path.isEmpty()) ThemedColorSchemeKeyTokens.Error else ThemedColorSchemeKeyTokens.Outline).value
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(SizeTokens.Level36)
                        .width(SizeTokens.Level1)
                        .fillMaxHeight()
                )
                CheckIconButton(enabled = _enabled, checked = checked ?: item.extraInfo.activated, onCheckedChange = onCheckedChange)
            }

            AnimatedVisibility(visible = filterMode, enter = fadeIn() + slideInVertically(), exit = slideOutVertically() + fadeOut()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingStart(SizeTokens.Level64)
                        .paddingBottom(SizeTokens.Level16),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(-SizeTokens.Level8),
                    content = {
                        val storageStatsFormat = item.displayStatsBytes

                        if (existed.not()) {
                            AssistChip(
                                enabled = true,
                                label = stringResource(id = R.string.not_exist),
                                leadingIcon = Icons.Outlined.Close,
                                trailingIcon = null,
                                color = ThemedColorSchemeKeyTokens.Error,
                                containerColor = ThemedColorSchemeKeyTokens.ErrorContainer,
                                border = null,
                            )
                        }

                        if (item.preserveId != 0L) {
                            AssistChip(
                                enabled = true,
                                label = stringResource(id = R.string._protected),
                                leadingIcon = Icons.Outlined.Shield,
                                trailingIcon = null,
                                color = ThemedColorSchemeKeyTokens.YellowPrimary,
                                containerColor = ThemedColorSchemeKeyTokens.YellowPrimaryContainer,
                                border = null,
                            )
                        }
                        if (existed) {
                            AssistChip(
                                enabled = true,
                                label = storageStatsFormat.formatSize(),
                                leadingIcon = Icons.Outlined.Folder,
                                trailingIcon = null,
                                color = ThemedColorSchemeKeyTokens.Primary,
                                containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
                                border = null,
                            )
                        }
                    }
                )
            }
        }
    }
}
