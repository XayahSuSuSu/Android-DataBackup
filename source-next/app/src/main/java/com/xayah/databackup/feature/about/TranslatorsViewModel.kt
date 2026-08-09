package com.xayah.databackup.feature.about

import com.xayah.databackup.data.Translator
import com.xayah.databackup.data.TranslatorHttpException
import com.xayah.databackup.data.TranslatorRepository
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val Translator.hasCompleteProfile: Boolean
    get() = !name.isNullOrBlank() && !avatar.isNullOrBlank() && !link.isNullOrBlank()

private val TranslatorComparator =
    compareByDescending<Translator> { it.hasCompleteProfile }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.username }

sealed interface TranslatorsStatus {
    data object Loading : TranslatorsStatus
    data object Ready : TranslatorsStatus
    data class Error(val httpStatusCode: Int? = null) : TranslatorsStatus
}

data class TranslatorsUiState(
    val status: TranslatorsStatus = TranslatorsStatus.Loading,
    val translators: List<Translator> = emptyList(),
)

class TranslatorsViewModel(
    private val translatorRepository: TranslatorRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "TranslatorsViewModel"
    }

    private val _uiState = MutableStateFlow(TranslatorsUiState())
    val uiState: StateFlow<TranslatorsUiState> = _uiState.asStateFlow()

    fun initialize() {
        withLock(Dispatchers.IO) {
            if (_uiState.value.status != TranslatorsStatus.Loading) return@withLock
            loadTranslators()
        }
    }

    fun refresh() {
        withLock(Dispatchers.IO) {
            loadTranslators()
        }
    }

    private suspend fun loadTranslators() {
        _uiState.update { it.copy(status = TranslatorsStatus.Loading) }
        runCatching {
            translatorRepository.getTranslators()
        }.onSuccess { translators ->
            _uiState.update {
                it.copy(
                    status = TranslatorsStatus.Ready,
                    translators = translators.sortedWith(TranslatorComparator),
                )
            }
        }.onFailure { error ->
            LogHelper.e(TAG, "loadTranslators", "Failed to fetch translators.", error)
            _uiState.update {
                it.copy(
                    status = TranslatorsStatus.Error(
                        httpStatusCode = (error as? TranslatorHttpException)?.statusCode,
                    ),
                )
            }
        }
    }
}
