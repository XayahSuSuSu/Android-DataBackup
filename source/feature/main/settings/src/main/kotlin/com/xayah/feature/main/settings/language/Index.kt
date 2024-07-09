package com.xayah.feature.main.settings.language

import android.app.Activity
import android.content.ContextWrapper
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
import com.xayah.core.datastore.saveAppLanguage
import com.xayah.core.provider.LanguageProvider
import com.xayah.core.ui.component.Checkable
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.settings.R
import com.xayah.feature.main.settings.SettingsScaffold
import java.util.Locale

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageLanguageSelector() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLanguage = remember(uiState) { mutableStateOf(uiState.selectedLanguage) }

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.app_language),
    ) {
        fun onLangSelected(item: String) {
            viewModel.launchOnIO {
                selectedLanguage.value = item
                context.saveAppLanguage(item)
                viewModel.emitIntent(IndexUiIntent.UpdateLanguage)
            }

            viewModel.launchOnMain {
                ((context as ContextWrapper).baseContext as Activity).recreate()
            }
        }

        LazyColumn {
            item {
                Checkable(
                    enabled = selectedLanguage.value != ConstantUtil.LANGUAGE_SYSTEM,
                    title = StringResourceToken.fromStringId(R.string.system),
                    checked = selectedLanguage.value == ConstantUtil.LANGUAGE_SYSTEM,
                    value = StringResourceToken.StringToken(ConstantUtil.LANGUAGE_SYSTEM),
                    onCheckedChange = { onLangSelected(ConstantUtil.LANGUAGE_SYSTEM) },
                )
            }

            items(count = BuildConfigUtil.SUPPORTED_LOCALES.size) {
                val item = BuildConfigUtil.SUPPORTED_LOCALES[it]
                val locale = LanguageProvider.convertStringToLocale(item)

                Checkable(
                    enabled = selectedLanguage.value != item,
                    title = StringResourceToken.StringToken(locale.getDisplayName(locale)),
                    checked = selectedLanguage.value == item,
                    value = StringResourceToken.StringToken(item),
                    onCheckedChange = { onLangSelected(item) },
                )
            }
        }
    }
}