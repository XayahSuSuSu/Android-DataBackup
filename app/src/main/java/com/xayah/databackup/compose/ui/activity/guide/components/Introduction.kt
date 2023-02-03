package com.xayah.databackup.compose.ui.activity.guide.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.GuideType

data class IntroductionItem(
    @StringRes val titleId: Int,
    @StringRes val subtitleId: Int,
    @StringRes val contentId: Int,
)

@ExperimentalMaterial3Api
@Composable
fun Introduction(setType: (GuideType) -> Unit) {
    GuideScaffold(
        title = stringResource(id = R.string.welcome_to_use),
        icon = Icons.Rounded.AccountCircle,
        showBtnIcon = true,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = {
            setType(GuideType.Update)
        },
        items = {
            val introductionList = mutableListOf(
                IntroductionItem(
                    R.string.start,
                    R.string.start_subtitle,
                    R.string.start_desc,
                ),
                IntroductionItem(
                    R.string.network,
                    R.string.network_subtitle,
                    R.string.network_desc,
                ),
                IntroductionItem(
                    R.string.application,
                    R.string.application_subtitle,
                    R.string.application_desc,
                ),
                IntroductionItem(
                    R.string.storage,
                    R.string.storage_subtitle,
                    R.string.storage_desc,
                ),
            )
            items(count = introductionList.size) {
                PermissionCard(
                    serial = "${it + 1}",
                    title = stringResource(id = introductionList[it].titleId),
                    subtitle = stringResource(id = introductionList[it].subtitleId),
                    content = stringResource(id = introductionList[it].contentId)
                )
            }
        }
    )
}