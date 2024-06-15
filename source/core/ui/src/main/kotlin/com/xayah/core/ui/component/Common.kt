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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.model.OpType
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.getValue
import com.xayah.core.ui.util.value
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import com.xayah.core.util.withIOContext
import kotlin.math.min

@ExperimentalMaterial3Api
@Composable
fun Section(title: StringResourceToken, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.paddingHorizontal(SizeTokens.Level16), verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)) {
        LabelLargeText(text = title.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), fontWeight = FontWeight.SemiBold)

        content()
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVectorToken,
    colorContainer: ColorSchemeKeyTokens,
    colorL80D20: ColorSchemeKeyTokens,
    onColorContainer: ColorSchemeKeyTokens,
    trailingIcon: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(SizeTokens.Level18),
        colors = CardDefaults.cardColors(containerColor = colorContainer.toColor(enabled)),
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
                color = colorL80D20.toColor(enabled),
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ) {
                Icon(
                    modifier = Modifier.padding(SizeTokens.Level8),
                    imageVector = icon.value,
                    tint = onColorContainer.toColor(enabled),
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
                    color = ColorSchemeKeyTokens.PrimaryContainer.toColor(),
                    onClick = onClick,
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LabelMediumText(text = "${packages[index].packageInfo.label.firstOrNull() ?: ""}", color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor())
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
                    color = ColorSchemeKeyTokens.PrimaryContainer.toColor(),
                    onClick = onClick,
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LabelMediumText(text = "${packages[icons.lastIndex].packageInfo.label.firstOrNull() ?: ""}", color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor())
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
                color = ColorSchemeKeyTokens.PrimaryContainer.toColor(),
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LabelMediumText(text = "+${packages.size - maxDisplayNum + 1}", color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor())
                }
            }
        }
    }
}


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
                PackageIconImage(packageName = item.packageName, label = "${item.packageInfo.label.firstOrNull() ?: ""}", size = SizeTokens.Level32)
                Column(modifier = Modifier.weight(1f)) {
                    TitleLargeText(
                        text = item.packageInfo.label.ifEmpty { StringResourceToken.fromStringId(R.string.unknown).getValue(context) },
                        color = (if (item.preserveId != 0L) ColorSchemeKeyTokens.YellowPrimary else ColorSchemeKeyTokens.OnSurface).toColor()
                    )
                    BodyMediumText(
                        text = StringResourceToken.fromString(item.packageName).value,
                        color = ColorSchemeKeyTokens.Outline.toColor()
                    )
                    BodyMediumText(
                        text = (
                                if (item.preserveId == 0L) {
                                    StringResourceToken.fromStringArgs(
                                        StringResourceToken.fromStringId(R.string.user),
                                        StringResourceToken.fromString(": ${item.userId}"),
                                    )
                                } else {
                                    StringResourceToken.fromStringArgs(
                                        StringResourceToken.fromStringId(R.string.user),
                                        StringResourceToken.fromString(": ${item.userId}, "),
                                        StringResourceToken.fromStringId(R.string.id),
                                        StringResourceToken.fromString(": ${item.preserveId}"),
                                    )
                                }
                                ).value,
                        color = ColorSchemeKeyTokens.OnSurface.toColor()
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

            AnimatedVisibility(visible = filterMode, enter = fadeIn() + slideInVertically(), exit = slideOutVertically() + fadeOut()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingStart(SizeTokens.Level64)
                        .paddingBottom(SizeTokens.Level16),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    content = {
                        val ssaid = item.extraInfo.ssaid
                        val hasKeystore = item.extraInfo.hasKeystore
                        val storageStatsFormat = when (item.indexInfo.opType) {
                            OpType.BACKUP -> item.storageStatsBytes
                            OpType.RESTORE -> item.displayStatsBytes
                        }

                        if (item.preserveId != 0L) {
                            AssistChip(
                                enabled = true,
                                label = StringResourceToken.fromStringId(R.string._protected),
                                leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Shield),
                                trailingIcon = null,
                                color = ColorSchemeKeyTokens.YellowPrimary,
                                containerColor = ColorSchemeKeyTokens.YellowPrimaryContainer,
                                border = null,
                            )
                        }
                        if (storageStatsFormat != (0).toDouble()) {
                            AssistChip(
                                enabled = true,
                                label = StringResourceToken.fromString(storageStatsFormat.formatSize()),
                                leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Folder),
                                trailingIcon = null,
                                color = ColorSchemeKeyTokens.Primary,
                                containerColor = ColorSchemeKeyTokens.PrimaryContainer,
                                border = null,
                            )
                        }
                        if (ssaid.isNotEmpty()) AssistChip(
                            enabled = true,
                            label = StringResourceToken.fromStringId(R.string.ssaid),
                            leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Pin),
                            trailingIcon = null,
                            color = ColorSchemeKeyTokens.Primary,
                            containerColor = ColorSchemeKeyTokens.PrimaryContainer,
                            border = null,
                        )
                        if (hasKeystore) AssistChip(
                            enabled = true,
                            label = StringResourceToken.fromStringId(R.string.keystore),
                            leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Key),
                            trailingIcon = null,
                            color = ColorSchemeKeyTokens.Primary,
                            containerColor = ColorSchemeKeyTokens.PrimaryContainer,
                            border = null,
                        )
                    }
                )
            }
        }
    }
}

@ExperimentalLayoutApi
@ExperimentalFoundationApi
@Composable
fun MediaItem(item: MediaEntity, enabled: Boolean, checked: Boolean? = null, onCheckedChange: ((Boolean) -> Unit)?, filterMode: Boolean, onClick: () -> Unit) {
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
                MediaIconImage(name = item.name.firstOrNull()?.toString() ?: "", size = SizeTokens.Level32)
                Column(modifier = Modifier.weight(1f)) {
                    TitleLargeText(
                        text = item.name.ifEmpty { StringResourceToken.fromStringId(R.string.unknown).getValue(context) },
                        color = (if (item.preserveId != 0L) ColorSchemeKeyTokens.YellowPrimary else ColorSchemeKeyTokens.OnSurface).toColor()
                    )
                    BodyMediumText(
                        text = StringResourceToken.fromString(item.path).value,
                        color = ColorSchemeKeyTokens.Outline.toColor()
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(SizeTokens.Level36)
                        .width(SizeTokens.Level1)
                        .fillMaxHeight()
                )
                CheckIconButton(enabled = enabled, checked = checked ?: item.extraInfo.activated, onCheckedChange = onCheckedChange)
            }

            AnimatedVisibility(visible = filterMode, enter = fadeIn() + slideInVertically(), exit = slideOutVertically() + fadeOut()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingStart(SizeTokens.Level64)
                        .paddingBottom(SizeTokens.Level16),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    content = {
                        val storageStatsFormat = item.displayStatsBytes

                        if (enabled.not()) {
                            AssistChip(
                                enabled = true,
                                label = StringResourceToken.fromStringId(R.string.not_exist),
                                leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Close),
                                trailingIcon = null,
                                color = ColorSchemeKeyTokens.Error,
                                containerColor = ColorSchemeKeyTokens.ErrorContainer,
                                border = null,
                            )
                        }

                        if (item.preserveId != 0L) {
                            AssistChip(
                                enabled = true,
                                label = StringResourceToken.fromStringId(R.string._protected),
                                leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Shield),
                                trailingIcon = null,
                                color = ColorSchemeKeyTokens.YellowPrimary,
                                containerColor = ColorSchemeKeyTokens.YellowPrimaryContainer,
                                border = null,
                            )
                        }
                        if (enabled) {
                            AssistChip(
                                enabled = true,
                                label = StringResourceToken.fromString(storageStatsFormat.formatSize()),
                                leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Folder),
                                trailingIcon = null,
                                color = ColorSchemeKeyTokens.Primary,
                                containerColor = ColorSchemeKeyTokens.PrimaryContainer,
                                border = null,
                            )
                        }
                    }
                )
            }
        }
    }
}
