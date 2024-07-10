package com.xayah.feature.main.settings.language

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.LanguageUtil.toLocale
import com.xayah.feature.main.settings.R
import com.xayah.feature.main.settings.SettingsScaffold

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageLanguageSelector() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.language),
    ) {
        val sortedLocales = remember { BuildConfigUtil.SUPPORTED_LOCALES.sorted() }
        LazyColumn {
            items(count = sortedLocales.size + 1) {
                if (it == 0) {
                    Clickable(
                        enabled = selectedLanguage != ConstantUtil.LANGUAGE_SYSTEM,
                        title = StringResourceToken.fromStringId(R.string.system),
                        onClick = { viewModel.emitIntentOnIO(IndexUiIntent.UpdateLanguage(navController, ConstantUtil.LANGUAGE_SYSTEM)) },
                    )
                } else {
                    val item = sortedLocales[it - 1]
                    val locale by remember(item) { mutableStateOf(item.toLocale(context)) }
                    Clickable(
                        enabled = selectedLanguage != item,
                        title = StringResourceToken.StringToken(locale.getDisplayName(locale)),
                        onClick = { viewModel.emitIntentOnIO(IndexUiIntent.UpdateLanguage(navController, item)) },
                    )
                }
            }
        }
    }
}
