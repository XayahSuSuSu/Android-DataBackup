package com.xayah.feature.main.restore

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.OverviewCard
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.core.util.DateUtil

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun RestoreScaffold(scrollBehavior: TopAppBarScrollBehavior, title: StringResourceToken, actions: @Composable RowScope.() -> Unit = {}, content: @Composable (BoxScope.() -> Unit)) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
                actions = actions
            )
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = content)
        }
    }
}

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewLastRestoreCard(modifier: Modifier, lastRestoreTime: Long) {
    val context = LocalContext.current
    val relativeTime by remember(lastRestoreTime) {
        mutableStateOf(DateUtil.getShortRelativeTimeSpanString(context = context, time1 = lastRestoreTime, time2 = DateUtil.getTimestamp()))
    }
    val finishTime by remember(lastRestoreTime) {
        mutableStateOf(context.getString(R.string.args_finished_at, DateUtil.formatTimestamp(lastRestoreTime, DateUtil.PATTERN_FINISH)))
    }
    OverviewCard(
        modifier = modifier,
        title = StringResourceToken.fromStringId(R.string.last_restore),
        icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_package_2),
        colorContainer = ColorSchemeKeyTokens.PrimaryContainer,
        onColorContainer = ColorSchemeKeyTokens.OnPrimaryContainer,
        content = {
            TitleLargeText(
                text = (if (lastRestoreTime == 0L) StringResourceToken.fromStringId(R.string.never) else StringResourceToken.fromString(relativeTime)).value,
                color = ColorSchemeKeyTokens.OnSurface.toColor()
            )
            if (lastRestoreTime != 0L)
                BodyMediumText(
                    text = StringResourceToken.fromString(finishTime).value,
                    color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                )
        },
        actionIcon = null,
    )
}
