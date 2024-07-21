package com.xayah.feature.main.settings.about

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.token.SizeTokens
import com.xayah.feature.main.settings.R
import com.xayah.feature.main.settings.SettingsScaffold
import com.xayah.feature.main.settings.TranslatorCard

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageTranslatorsSettings() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Load)
    }

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.translators),
        actions = {}
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
        ) {
            item {
                Spacer(modifier = Modifier.size(SizeTokens.Level0))
            }

            items(items = uiState.translators) { item ->
                item.keys.forEach { lang ->
                    Title(title = lang) {
                        Column(verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)) {
                            if (item[lang] != null) {
                                if (item[lang]!!.isEmpty()) {
                                    TranslatorCard(
                                        modifier = Modifier.paddingHorizontal(SizeTokens.Level16),
                                        avatar = null,
                                        name = context.getString(R.string.unknown),
                                        desc = context.getString(R.string.translators_desc)
                                    ) {
                                    }
                                } else {
                                    item[lang]!!.forEach { info ->
                                        TranslatorCard(
                                            modifier = Modifier.paddingHorizontal(SizeTokens.Level16),
                                            avatar = info.getOrNull(3),
                                            name = info.getOrNull(1) ?: context.getString(R.string.unknown),
                                            desc = info.getOrNull(0) ?: context.getString(R.string.unknown)
                                        ) {
                                            val url = info.getOrNull(4)
                                            if (url != null) {
                                                viewModel.emitIntentOnIO(IndexUiIntent.ToBrowser(context, url))
                                            }
                                        }
                                    }
                                }
                            }

                        }

                    }

                }
            }

            item {
                InnerBottomSpacer(innerPadding = it)
            }
        }
    }
}
