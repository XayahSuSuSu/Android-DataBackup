package com.xayah.databackup.ui.activity.main.page.reload

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.databackup.R
import com.xayah.databackup.util.command.ConfigsUtil
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReloadUiState(
    val isLoading: Boolean,
    val packageRestoreEntireDao: PackageRestoreEntireDao,
    val configsUtil: ConfigsUtil,
    val snackbarHostState: SnackbarHostState,
    val selectedIndex: Int,
    val options: List<String>,
    val packages: List<PackageRestoreEntire>,
    val medium: List<MediaRestoreEntity>,
)

@HiltViewModel
class ReloadViewModel @Inject constructor(
    @ApplicationContext context: Context,
    configsUtilFactory: ConfigsUtil.IConfigsUtilFactory,
    packageRestoreEntireDao: PackageRestoreEntireDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        ReloadUiState(
            isLoading = false,
            packageRestoreEntireDao = packageRestoreEntireDao,
            configsUtil = configsUtilFactory.create(cloudMode = savedStateHandle.get<Boolean>("cloudMode") ?: false),
            snackbarHostState = SnackbarHostState(),
            selectedIndex = 0,
            options = listOf(context.getString(R.string.speed_mode), context.getString(R.string.recursive_mode)),
            packages = listOf(),
            medium = listOf(),
        )
    )
    val uiState: State<ReloadUiState>
        get() = _uiState

    private fun setPackages(packages: List<PackageRestoreEntire>) {
        _uiState.value = uiState.value.copy(packages = packages)
    }

    private fun setMedium(medium: List<MediaRestoreEntity>) {
        _uiState.value = uiState.value.copy(medium = medium)
    }

    fun setSelectedIndex(index: Int) {
        _uiState.value = uiState.value.copy(selectedIndex = index)
    }

    private fun setLoading(value: Boolean) {
        _uiState.value = uiState.value.copy(isLoading = value)
    }

    fun initialize() {
        viewModelScope.launch {
            withIOContext {
                val uiState by uiState
                if (uiState.isLoading.not()) {
                    setLoading(true)

                    when (uiState.selectedIndex) {
                        0 -> {
                            setPackages(listOf())
                            setMedium(listOf())
                            val (packages, medium) = uiState.configsUtil.dumpConfigs()
                            setPackages(packages)
                            setMedium(medium)
                        }

                        1 -> {
                            setPackages(listOf())
                            setMedium(listOf())
                            val packages = uiState.configsUtil.dumpPackageConfigsRecursively()
                            val medium = uiState.configsUtil.dumpMediaConfigsRecursively()
                            setPackages(packages)
                            setMedium(medium)
                        }
                    }

                    setLoading(false)
                }
            }
        }
    }

    suspend fun reloading(context: Context) {
        withIOContext {
            val uiState by uiState
            uiState.configsUtil.restoreIcons()
            uiState.configsUtil.restoreConfigs(uiState.packages, uiState.medium)
            uiState.snackbarHostState.showSnackbar(
                message = context.getString(R.string.succeed),
                duration = SnackbarDuration.Short
            )
        }
    }
}
