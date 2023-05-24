package com.xayah.databackup.ui.activity.main.page.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.component.IntroCard
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens

@ExperimentalMaterial3Api
@Composable
fun PageIntro() {
    val titles = listOf(
        stringResource(id = R.string.start),
        stringResource(id = R.string.network),
        stringResource(id = R.string.application),
        stringResource(id = R.string.storage)
    )
    val subtitles = listOf(
        stringResource(id = R.string.start_subtitle),
        stringResource(id = R.string.network_subtitle),
        stringResource(id = R.string.application_subtitle),
        stringResource(id = R.string.storage_subtitle)
    )
    val contents = listOf(
        stringResource(id = R.string.start_desc),
        stringResource(id = R.string.network_desc),
        stringResource(id = R.string.application_desc),
        stringResource(id = R.string.storage_desc)
    )

    LazyColumn(
        modifier = Modifier.paddingTop(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
    ) {
        items(count = titles.size, key = { it }) {
            IntroCard(
                serial = (it + 1).digitToChar(),
                title = titles[it],
                subtitle = subtitles[it],
                content = contents[it]
            )
        }
        item {
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingSmall))
        }
    }
}
