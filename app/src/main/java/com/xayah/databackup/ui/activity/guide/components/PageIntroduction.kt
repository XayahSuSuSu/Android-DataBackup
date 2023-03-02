package com.xayah.databackup.ui.activity.guide.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.ui.activity.guide.GuideViewModel
import com.xayah.databackup.ui.activity.guide.components.card.CardPermission

@ExperimentalMaterial3Api
@Composable
fun PageIntroduction(viewModel: GuideViewModel) {
    GuideScaffold(
        title = stringResource(id = R.string.welcome_to_use),
        icon = Icons.Rounded.AccountCircle,
        showBtnIcon = true,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = {
            viewModel.initType.value = GuideType.Update
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
            items(count = introductionList.size, key = { introductionList[it].titleId }) {
                CardPermission(
                    serial = "${it + 1}",
                    title = stringResource(id = introductionList[it].titleId),
                    subtitle = stringResource(id = introductionList[it].subtitleId),
                    content = stringResource(id = introductionList[it].contentId)
                )
            }
        }
    )
}