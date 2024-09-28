package com.xayah.feature.main.settings.about

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.ui.component.AppIcon
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.FilledTonalIconTextButton
import com.xayah.core.ui.component.HeadlineSmallText
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.OutlinedButtonIconTextButton
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.joinOf
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.settings.ContributorCard
import com.xayah.feature.main.settings.R
import com.xayah.feature.main.settings.SettingsScaffold

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageAboutSettings() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Load)
    }

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.about),
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(modifier = Modifier.paddingVertical(SizeTokens.Level16))
                HeadlineSmallText(text = stringResource(id = R.string.app_name), color = ThemedColorSchemeKeyTokens.OnSurface.value)
                BodyMediumText(
                    text = joinOf(
                        stringResource(id = R.string.version),
                        " ${BuildConfigUtil.VERSION_NAME} ",
                        "(${BuildConfigUtil.VERSION_CODE})",
                    ),
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                )
                FlowRow(
                    modifier = Modifier.paddingTop(SizeTokens.Level16),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                    maxItemsInEachRow = 2
                ) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                        var expanded by remember { mutableStateOf(false) }
                        FilledTonalIconTextButton(
                            modifier = Modifier.width(SizeTokens.Level128),
                            icon = Icons.Outlined.FavoriteBorder,
                            text = stringResource(id = R.string.donate)
                        ) {
                            expanded = true
                        }
                        ModalActionDropdownMenu(expanded = expanded, actionList = listOf(
//                            ActionMenuItem(
//                                title = stringResource(id = R.string.buymeacoffee),
//                                enabled = true,
//                                secondaryMenu = listOf(),
//                                onClick = {
//                                    viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, ConstantUtil.DONATE_BMAC_LINK))
//                                }
//                            ),
                            ActionMenuItem(
                                title = stringResource(id = R.string.paypal),
                                enabled = true,
                                secondaryMenu = listOf(),
                                onClick = {
                                    viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, ConstantUtil.DONATE_PAYPAL_LINK))
                                }
                            ),
                            ActionMenuItem(
                                title = stringResource(id = R.string.afdian),
                                enabled = true,
                                secondaryMenu = listOf(),
                                onClick = {
                                    viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, ConstantUtil.DONATE_AFD_LINK))
                                }
                            )
                        ), onDismissRequest = { expanded = false })
                    }
                    FilledTonalIconTextButton(
                        modifier = Modifier.width(SizeTokens.Level128),
                        icon = Icons.Outlined.Assignment,
                        text = stringResource(id = R.string.docs)
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, ConstantUtil.DOC_LINK))
                    }
                    OutlinedButtonIconTextButton(
                        modifier = Modifier.width(SizeTokens.Level128),
                        icon = Icons.Outlined.Code,
                        text = stringResource(id = R.string.github)
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, ConstantUtil.GITHUB_LINK))
                    }
                    OutlinedButtonIconTextButton(
                        modifier = Modifier.width(SizeTokens.Level128),
                        icon = Icons.Outlined.Chat,
                        text = stringResource(id = R.string.contact)
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, ConstantUtil.CHAT_LINK))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .paddingHorizontal(SizeTokens.Level16),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
            ) {
                uiState.contributors.forEach {
                    ContributorCard(
                        avatar = it.avatar, name = it.name, desc = it.desc
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, it.link))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Clickable(
                    title = stringResource(id = R.string.translators),
                    value = stringResource(id = R.string.translators_desc),
                ) {
                    navController.navigateSingle(MainRoutes.Translators.route)
                }
            }

            InnerBottomSpacer(innerPadding = it)
        }
    }
}
