package com.xayah.feature.main.settings.about

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.gson.reflect.TypeToken
import com.xayah.core.model.ContributorItem
import com.xayah.core.model.MutableWeblateItems
import com.xayah.core.model.TranslatorRevisionItem
import com.xayah.core.model.WeblateItems
import com.xayah.core.model.WeblateSrcItems
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
    val translators: WeblateItems,
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
                val translators: WeblateSrcItems = gsonUtil.fromJson(
                    context.resources.openRawResource(R.raw.translators).bufferedReader().readText(),
                    object : TypeToken<WeblateSrcItems>() {}.type
                )
                val translatorsRevision: Map<String, TranslatorRevisionItem> = gsonUtil.fromJson(
                    context.resources.openRawResource(R.raw.translators_revision).bufferedReader().readText(),
                    object : TypeToken<Map<String, TranslatorRevisionItem>>() {}.type
                )
                val translatorsRevised: MutableWeblateItems = mutableListOf()
                translators.forEach { langMap ->
                    val mutableLangMap: MutableMap<String, MutableList<MutableList<String>>> = mutableMapOf()
                    langMap.forEach { (lang, translatorList) ->
                        val mutableTranslatorList: MutableList<MutableList<String>> = mutableListOf()
                        translatorList.forEach { translator ->
                            val email = translator.email
                            if (translatorsRevision.containsKey(email)) {
                                val revisionItem = translatorsRevision[email]!!
                                if (revisionItem.actions.containsKey(lang)) {
                                    // Need revision
                                    when (translatorsRevision[email]!!.actions[lang]!!) {
                                        0 -> {
                                            // Replace
                                            val newTranslator = mutableListOf(translator.email, translator.fullName, translator.changeCount)
                                            newTranslator[1] = revisionItem.name
                                            newTranslator.add(revisionItem.avatar)
                                            newTranslator.add(revisionItem.link)
                                            mutableTranslatorList.add(newTranslator)
                                        }

                                        1 -> {
                                            // Skip
                                        }
                                    }
                                }
                            } else {
                                mutableTranslatorList.add(mutableListOf(translator.email, translator.fullName, translator.changeCount))
                            }
                        }
                        mutableLangMap[lang] = mutableTranslatorList
                    }
                    translatorsRevised.add(mutableLangMap)
                }

                emitState(
                    state.copy(
                        contributors = gsonUtil.fromJson(
                            context.resources.openRawResource(R.raw.contributors).bufferedReader().readText(),
                            object : TypeToken<List<ContributorItem>>() {}.type
                        ),
                        translators = translatorsRevised
                    )
                )
            }

            is IndexUiIntent.ToBrowser -> {
                runCatching { intent.context.toBrowser(intent.url) }.onFailure { emitEffect(IndexUiEffect.ShowSnackbar(message = context.getString(R.string.no_browser))) }
            }
        }
    }
}
