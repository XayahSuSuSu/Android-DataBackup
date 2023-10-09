package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookCloudCard
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun PageMain(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
            OverLookCloudCard()
        }

        item {
            Module(title = stringResource(R.string.utilities)) {
                val actions = listOf(
                    context.getString(R.string.account),
                )
                val icons = listOf(
                    ImageVector.vectorResource(R.drawable.ic_rounded_badge),
                )
                val onClicks = listOf<suspend () -> Unit>(
                    {
                        navController.navigate(CloudRoutes.Account.route)
                    },
                )
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
                    Row(horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)) {
                        repeat(actions.size) { index ->
                            CardActionButton(
                                text = actions[index],
                                icon = icons[index],
                                onClick = {
                                    scope.launch {
                                        onClicks[index]()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
                }
            }
        }
    }
}
