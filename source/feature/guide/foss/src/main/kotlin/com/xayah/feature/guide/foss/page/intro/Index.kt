package com.xayah.feature.guide.foss.page.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.guide.common.IntroCard
import com.xayah.feature.guide.foss.GuideRoutes
import com.xayah.feature.guide.common.LocalMainViewModel
import com.xayah.feature.guide.common.MainUiIntent
import com.xayah.feature.guide.common.MainUiState
import com.xayah.feature.guide.foss.R

@ExperimentalMaterial3Api
@Composable
fun PageIntro(navController: NavHostController) {
    val mainViewModel = LocalMainViewModel.current!!
    LaunchedEffect(null) {
        mainViewModel.emitIntent(
            MainUiIntent.SetUiState(
                state = MainUiState(
                    isInitializing = false,
                    topBarTitle = StringResourceToken.fromStringId(R.string.welcome_to_use),
                    topBarIcon = ImageVectorToken.fromVector(Icons.Rounded.AccountCircle),
                    fabIcon = ImageVectorToken.fromVector(Icons.Rounded.ArrowForward),
                    onFabClick = {
                        navController.navigate(GuideRoutes.Env.route)
                    }
                )
            )
        )
    }


    val titles = remember {
        listOf(
            StringResourceToken.fromStringId(id = R.string.start),
            StringResourceToken.fromStringId(id = R.string.application),
            StringResourceToken.fromStringId(id = R.string.storage),
        )
    }
    val subtitles = remember {
        listOf(
            StringResourceToken.fromStringId(id = R.string.start_subtitle),
            StringResourceToken.fromStringId(id = R.string.application_subtitle),
            StringResourceToken.fromStringId(id = R.string.storage_subtitle),
        )
    }
    val contents = remember {
        listOf(
            StringResourceToken.fromStringId(id = R.string.start_desc),
            StringResourceToken.fromStringId(id = R.string.application_desc),
            StringResourceToken.fromStringId(id = R.string.storage_desc),
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
        item {
            Spacer(modifier = Modifier.height(PaddingTokens.Level1))
        }

        items(count = titles.size, key = { it }) {
            IntroCard(
                serial = (it + 1).digitToChar(),
                title = titles[it],
                subtitle = subtitles[it],
                content = contents[it]
            )
        }

        item {
            Spacer(modifier = Modifier.height(PaddingTokens.Level1))
        }
    }
}
