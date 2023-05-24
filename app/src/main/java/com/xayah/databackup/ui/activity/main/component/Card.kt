package com.xayah.databackup.ui.activity.main.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.BodyMediumBoldText
import com.xayah.databackup.ui.component.BodySmallBoldText
import com.xayah.databackup.ui.component.IconTextButton
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.TitleLargeText
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.State

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
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
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
