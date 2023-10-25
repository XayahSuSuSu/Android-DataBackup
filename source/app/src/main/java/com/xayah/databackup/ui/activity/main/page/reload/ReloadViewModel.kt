package com.xayah.databackup.ui.activity.main.page.reload

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.model.CompressionType
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.ConfigsUtil
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReloadUiState(
    val isLoading: Boolean,
    val packageRestoreEntireDao: PackageRestoreEntireDao,
    val snackbarHostState: SnackbarHostState,
    val packagesDir: String,
    val mediumDir: String,
    val configsDir: String,
    val selectedIndex: Int,
    val options: List<String>,
    val packages: List<PackageRestoreEntire>,
    val medium: List<MediaRestoreEntity>,
)

@HiltViewModel
class ReloadViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val configsUtil: ConfigsUtil,
    packageRestoreEntireDao: PackageRestoreEntireDao,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        ReloadUiState(
            isLoading = false,
            packageRestoreEntireDao = packageRestoreEntireDao,
            snackbarHostState = SnackbarHostState(),
            packagesDir = PathUtil.getRestorePackagesSavePath(),
            mediumDir = PathUtil.getRestoreMediumSavePath(),
            configsDir = PathUtil.getRestoreConfigsSavePath(),
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
                            val (packages, medium) = configsUtil.dumpConfigs("${uiState.configsDir}/configs.${CompressionType.TAR.suffix}")
                            setPackages(packages)
                            setMedium(medium)
                        }

                        1 -> {
                            setPackages(listOf())
                            setMedium(listOf())
                            val packages = configsUtil.dumpPackageConfigsRecursively(uiState.packagesDir)
                            val medium = configsUtil.dumpMediaConfigsRecursively(uiState.mediumDir)
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
            configsUtil.restoreIcons("${uiState.configsDir}/icon.${CompressionType.TAR.suffix}")
            configsUtil.restoreConfigs(uiState.packages, uiState.medium)
            uiState.snackbarHostState.showSnackbar(
                message = context.getString(R.string.succeed),
                duration = SnackbarDuration.Short
            )
        }
    }
}
