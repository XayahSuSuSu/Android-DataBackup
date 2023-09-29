package com.xayah.databackup.ui.component

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationState
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.ButtonTokens
import com.xayah.databackup.ui.token.CardTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.State
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.readLastBackupTime
import com.xayah.databackup.util.readLastRestoringTime
import com.xayah.librootservice.util.ExceptionUtil.tryOn

@Composable
fun IntroCard(serial: Char, title: String, subtitle: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(CommonTokens.PaddingMedium)) {
            Serial(serial = serial)
            TitleLargeText(modifier = Modifier.paddingVertical(CommonTokens.PaddingSmall), text = title)
            BodySmallBoldText(modifier = Modifier.paddingVertical(CommonTokens.PaddingSmall), text = subtitle)
            BodySmallBoldText(modifier = Modifier.paddingVertical(CommonTokens.PaddingSmall), text = content)
        }
    }
}

@Composable
fun UpdateCard(content: String, version: String, link: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(CommonTokens.PaddingMedium)) {
            BodySmallBoldText(text = content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingTop(CommonTokens.PaddingMedium),
                horizontalArrangement = Arrangement.End
            ) {
                IconTextButton(icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_link), text = version) {
                    tryOn(
                        block = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        },
                        onException = {
                            Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun EnvCard(content: String, state: State, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = {
            if (state != State.Succeed) onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                State.Loading -> {
                    ColorScheme.surfaceVariant()
                }

                State.Succeed -> {
                    ColorScheme.green()
                }

                State.Failed -> {
                    ColorScheme.error()
                }
            }
        )
    ) {
        Column(Modifier.padding(CommonTokens.PaddingMedium)) {
            Icon(
                imageVector = when (state) {
                    State.Loading -> ImageVector.vectorResource(id = R.drawable.ic_rounded_star)
                    State.Succeed -> Icons.Rounded.Done
                    State.Failed -> Icons.Rounded.Close
                },
                contentDescription = null,
                tint = when (state) {
                    State.Loading -> ColorScheme.onSurfaceVariant()
                    State.Succeed -> ColorScheme.greenContainer()
                    State.Failed -> ColorScheme.errorContainer()
                },
                modifier = Modifier
                    .size(CommonTokens.IconSmallSize)
                    .paddingBottom(CommonTokens.PaddingSmall)
            )
            BodyMediumBoldText(
                modifier = Modifier.paddingTop(CommonTokens.PaddingSmall),
                text = content,
                color = when (state) {
                    State.Loading -> ColorScheme.onSurfaceVariant()
                    State.Succeed -> ColorScheme.greenContainer()
                    State.Failed -> ColorScheme.errorContainer()
                }
            )
        }
    }
}

@Composable
fun OverLookBackupCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = ColorScheme.primaryContainer())
    ) {
        Column(modifier = Modifier.padding(CardTokens.ContentPadding)) {
            Row(
                modifier = Modifier.paddingBottom(CardTokens.ContentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_token),
                    contentDescription = null,
                    modifier = Modifier.paddingEnd(ButtonTokens.IconTextButtonPadding),
                )
                Spacer(modifier = Modifier.weight(1f))
                LabelLargeExtraBoldText(text = stringResource(R.string.overlook))
            }
            BodySmallText(text = stringResource(R.string.last_backup))
            TitleLargeBoldText(text = remember {
                val time = context.readLastBackupTime()
                if (time == 0L) context.getString(R.string.none)
                else DateUtil.getShortRelativeTimeSpanString(context, time, DateUtil.getTimestamp())
            })
        }
    }
}

@Composable
fun OverLookRestoreCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = ColorScheme.primaryContainer())
    ) {
        Column(modifier = Modifier.padding(CardTokens.ContentPadding)) {
            Row(
                modifier = Modifier.paddingBottom(CardTokens.ContentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_token),
                    contentDescription = null,
                    modifier = Modifier.paddingEnd(ButtonTokens.IconTextButtonPadding),
                )
                Spacer(modifier = Modifier.weight(1f))
                LabelLargeExtraBoldText(text = stringResource(R.string.overlook))
            }
            BodySmallText(text = stringResource(R.string.last_restore))
            TitleLargeBoldText(text = remember {
                val time = context.readLastRestoringTime()
                if (time == 0L) context.getString(R.string.none)
                else DateUtil.getShortRelativeTimeSpanString(context, time, DateUtil.getTimestamp())
            })
        }
    }
}

data class OperationCardConfig(
    val type: DataType,
    var content: String,
    var state: OperationState,
    val icon: ImageVector,
) {
    val title = type.type.uppercase()
}

@Composable
private fun getActionIcon(state: OperationState): ImageVector = when (state) {
    OperationState.IDLE -> {
        ImageVector.vectorResource(R.drawable.ic_rounded_adjust_circle)
    }

    OperationState.SKIP -> {
        ImageVector.vectorResource(R.drawable.ic_rounded_not_started_circle)
    }

    OperationState.Processing -> {
        ImageVector.vectorResource(R.drawable.ic_rounded_pending_circle)
    }

    OperationState.DONE -> {
        ImageVector.vectorResource(R.drawable.ic_rounded_check_circle)
    }

    OperationState.ERROR -> {
        ImageVector.vectorResource(R.drawable.ic_rounded_cancel_circle)
    }
}

@Composable
private fun getActionColor(state: OperationState): Color = when (state) {
    OperationState.IDLE -> {
        ColorScheme.primary()
    }

    OperationState.SKIP -> {
        ColorScheme.primary()
    }

    OperationState.Processing -> {
        ColorScheme.yellow()
    }

    OperationState.DONE -> {
        ColorScheme.green()
    }

    OperationState.ERROR -> {
        ColorScheme.error()
    }
}

@Composable
fun OperationCard(title: String, content: String, state: OperationState, icon: ImageVector) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(CardTokens.OpCardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CardTokens.OpCardPadding)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape, modifier = Modifier
                        .padding(CardTokens.OpCardActionIconPadding)
                        .size(CardTokens.OpCardIconSize), color = ColorScheme.primary()
                ) {
                    Icon(
                        modifier = Modifier.padding(CardTokens.OpCardPadding),
                        imageVector = icon,
                        contentDescription = null,
                        tint = ColorScheme.onPrimary()
                    )
                }
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(CardTokens.OpCardActionIconSize)
                        .align(Alignment.BottomEnd),
                    color = ColorScheme.surface()
                ) {
                    Icon(
                        modifier = Modifier.padding(CardTokens.OpCardActionIconInternalPadding),
                        imageVector = getActionIcon(state = state),
                        contentDescription = null,
                        tint = getActionColor(state = state)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                TitleMediumBoldText(text = title)
                LabelSmallText(text = content)
            }
        }
    }
}
