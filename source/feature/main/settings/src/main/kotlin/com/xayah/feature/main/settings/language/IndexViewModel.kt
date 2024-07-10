package com.xayah.feature.main.settings.language

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavHostController
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.datastore.readLanguage
import com.xayah.core.datastore.saveLanguage
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.LanguageUtil.toLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data class UpdateLanguage(val navController: NavHostController, val lang: String) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) :
    BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.UpdateLanguage -> {
                context.saveLanguage(intent.lang)
                withMainContext {
                    intent.navController.popBackStack()
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.create(intent.lang.toLocale(context))
                    )
                }
            }
        }
    }

    private var _selectedLanguage: Flow<String> = context.readLanguage().flowOnIO()
    val selectedLanguage: StateFlow<String> = _selectedLanguage.stateInScope(ConstantUtil.LANGUAGE_SYSTEM)
}
