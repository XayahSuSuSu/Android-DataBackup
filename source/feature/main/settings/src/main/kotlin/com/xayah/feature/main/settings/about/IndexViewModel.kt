package com.xayah.feature.main.settings.about

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.gson.reflect.TypeToken
import com.xayah.core.model.ContributorItem
import com.xayah.core.model.TranslatorItem
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.toBrowser
import com.xayah.feature.main.settings.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class IndexUiState(
    val contributors: List<ContributorItem>,
    val translators: List<TranslatorItem>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Load : IndexUiIntent()
    data class ToBrowser(val context: Context, val url: String) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gsonUtil: GsonUtil,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(contributors = listOf(), translators = listOf())) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Load -> {
                emitState(
                    state.copy(
                        contributors = gsonUtil.fromJson(
                            context.resources.openRawResource(R.raw.contributors).bufferedReader().readText(),
                            object : TypeToken<List<ContributorItem>>() {}.type
                        ),
                        translators = gsonUtil.fromJson(
                            context.resources.openRawResource(R.raw.translators).bufferedReader().readText(),
                            object : TypeToken<List<TranslatorItem>>() {}.type
                        )
                    )
                )
            }

            is IndexUiIntent.ToBrowser -> {
                runCatching { intent.context.toBrowser(intent.url) }.onFailure { emitEffect(IndexUiEffect.ShowSnackbar(message = context.getString(R.string.no_browser))) }
            }
        }
    }
}
