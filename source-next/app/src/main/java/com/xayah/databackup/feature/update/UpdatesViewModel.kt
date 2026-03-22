package com.xayah.databackup.feature.update

import com.xayah.databackup.App
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.data.GitHubApiErrorKind
import com.xayah.databackup.data.GitHubApiException
import com.xayah.databackup.data.GitHubReleaseRepository
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import kotlin.math.max

sealed interface UpdatesStatus {
    data object Loading : UpdatesStatus
    data object Refreshing : UpdatesStatus
    data object UpToDate : UpdatesStatus
    data object UpdateAvailable : UpdatesStatus
    data class Error(
        val message: String,
        val rateLimitResetAtMillis: Long? = null,
        val rateLimitResetLabel: String? = null,
    ) : UpdatesStatus
}

data class UpdatesUiState(
    val status: UpdatesStatus = UpdatesStatus.Loading,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String = "",
    val latestNotes: String = "",
)

class UpdatesViewModel(
    private val gitHubReleaseRepository: GitHubReleaseRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "UpdatesViewModel"
        private const val MAX_ERROR_MESSAGE_LENGTH = 240
    }

    private val _uiState = MutableStateFlow(UpdatesUiState())
    val uiState: StateFlow<UpdatesUiState> = _uiState.asStateFlow()

    fun initialize() {
        if (_uiState.value.status != UpdatesStatus.Loading) return
        refresh()
    }

    fun refresh() {
        withLock(Dispatchers.IO) {
            val isInitialLoad = _uiState.value.latestVersion.isEmpty()
            _uiState.update {
                it.copy(
                    status = if (isInitialLoad) UpdatesStatus.Loading else UpdatesStatus.Refreshing,
                    latestNotes = "",
                )
            }

            runCatching {
                val latestRelease = gitHubReleaseRepository.getLatestRelease()
                val latestVersion = latestRelease.tagName
                val updateAvailable = if (latestVersion.isEmpty()) {
                    false
                } else {
                    compareVersions(latestVersion) > 0
                }

                _uiState.update {
                    it.copy(
                        status = if (updateAvailable) {
                            UpdatesStatus.UpdateAvailable
                        } else {
                            UpdatesStatus.UpToDate
                        },
                        latestNotes = latestRelease.body.orEmpty().trim(),
                        latestVersion = latestVersion,
                    )
                }
            }.onFailure { throwable ->
                LogHelper.e(TAG, "refresh", "Failed to fetch releases.", throwable)
                val app = App.application
                val apiError = throwable as? GitHubApiException
                if (apiError?.kind == GitHubApiErrorKind.RATE_LIMITED) {
                    val resetLabel = apiError.rateLimitResetAtMillis?.let { formatTime(it) }
                    val message = if (resetLabel != null) {
                        app.getString(R.string.github_api_rate_limited, resetLabel)
                    } else {
                        val errorMessage = apiError.message.takeIf { msg -> msg.isNotBlank() }
                            ?: throwable.message.takeIf { msg -> msg.isNotBlank() }
                            ?: app.getString(R.string.loading_failed)
                        sanitizeErrorMessage(errorMessage)
                    }
                    _uiState.update {
                        it.copy(
                            status = UpdatesStatus.Error(
                                message = message,
                                rateLimitResetAtMillis = apiError.rateLimitResetAtMillis,
                                rateLimitResetLabel = resetLabel,
                            ),
                            latestNotes = "",
                        )
                    }
                } else {
                    val errorMessage = apiError?.message?.takeIf { msg -> msg.isNotBlank() }
                        ?: throwable.message?.takeIf { msg -> msg.isNotBlank() }
                        ?: app.getString(R.string.loading_failed)
                    _uiState.update {
                        it.copy(
                            status = UpdatesStatus.Error(message = sanitizeErrorMessage(errorMessage)),
                            latestNotes = "",
                        )
                    }
                }
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val formatter = java.text.SimpleDateFormat(
            App.application.getString(R.string.time_format_pattern),
            java.util.Locale.getDefault()
        )
        return formatter.format(Date(timestamp))
    }

    private fun compareVersions(version: String): Int {
        val partsA = extractVersionParts(version)
        val partsB = extractVersionParts(BuildConfig.VERSION_NAME)
        val maxLength = max(partsA.size, partsB.size)

        for (index in 0 until maxLength) {
            val a = partsA.getOrElse(index) { 0 }
            val b = partsB.getOrElse(index) { 0 }
            if (a != b) {
                return a.compareTo(b)
            }
        }
        return 0
    }

    private fun extractVersionParts(version: String): List<Int> {
        val normalized = version
            .trim()
            .lowercase(java.util.Locale.US)
            .removePrefix("v")
        val parts = Regex("\\d+")
            .findAll(normalized)
            .mapNotNull { match -> match.value.toIntOrNull() }
            .toList()
        return parts.ifEmpty { listOf(0) }
    }

    private fun sanitizeErrorMessage(message: String): String {
        val normalized = message
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.isEmpty()) {
            return App.application.getString(R.string.loading_failed)
        }

        return if (normalized.length > MAX_ERROR_MESSAGE_LENGTH) {
            normalized.take(MAX_ERROR_MESSAGE_LENGTH).trimEnd() + "..."
        } else {
            normalized
        }
    }
}
