package com.xayah.databackup.ui.activity.guide.page.env

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.guide.page.GuideUiState
import com.xayah.databackup.ui.activity.guide.page.GuideViewModel
import com.xayah.databackup.ui.activity.main.MainActivity
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.command.releaseBin
import com.xayah.databackup.util.saveAppVersionName
import com.xayah.librootservice.util.ExceptionUtil
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import com.xayah.databackup.ui.token.State as TokenState

data class EnvItem(
    val content: String,
    var state: TokenState,
    val onClick: suspend (context: Context, viewModel: GuideViewModel, dialogSlot: DialogState) -> Unit,
)

data class EnvUiState(
    val mutex: Mutex,
    val envItems: List<EnvItem>,
    val allValidated: Boolean,
)

@HiltViewModel
class EnvViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {
    private val _uiState = mutableStateOf(
        EnvUiState(
            mutex = Mutex(),
            envItems = listOf(
                EnvItem(
                    content = context.getString(R.string.grant_root_access),
                    state = TokenState.Loading,
                    onClick = { context, viewModel, _ ->
                        runAndValidate(context, viewModel) {
                            ExceptionUtil.tryOnScope {
                                withIOContext {
                                    setState(index = 0, state = if (Shell.getShell().isRoot) TokenState.Succeed else TokenState.Failed)

                                    // Kill daemon
                                    PreparationUtil.killDaemon(context)
                                }
                            }
                        }
                    }
                ),
                EnvItem(
                    content = context.getString(R.string.release_prebuilt_binaries),
                    state = TokenState.Loading,
                    onClick = { context, viewModel, _ ->
                        runAndValidate(context, viewModel) {
                            ExceptionUtil.tryOnScope {
                                withIOContext {
                                    var isSucceed = true
                                    releaseBin(context).also {
                                        if (it.not()) isSucceed = false
                                    }

                                    setState(index = 1, state = if (isSucceed) TokenState.Succeed else TokenState.Failed)
                                }
                            }
                        }
                    }
                ),
                EnvItem(
                    content = context.getString(R.string.abi_validation),
                    state = TokenState.Loading,
                    onClick = { context, viewModel, dialogSlot ->
                        runAndValidate(context, viewModel) {
                            ExceptionUtil.tryOnScope {
                                withIOContext {
                                    val buildABI = BuildConfig.FLAVOR_abi
                                    val deviceABI = Build.SUPPORTED_ABIS.firstOrNull().toString()
                                    if (buildABI == deviceABI) {
                                        setState(index = 2, state = TokenState.Succeed)
                                    } else {
                                        setState(index = 2, state = TokenState.Succeed)
                                        dialogSlot.openConfirmDialog(
                                            context = context,
                                            text = context.getString(
                                                R.string.this_version_only_supports_but_your_device_is_please_install_version,
                                                buildABI,
                                                deviceABI,
                                                deviceABI
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            ),
            allValidated = false
        )
    )
    val uiState: State<EnvUiState>
        get() = _uiState

    private fun setState(index: Int, state: TokenState) {
        val uiState by uiState
        val envItems = uiState.envItems.toMutableList()
        envItems[index] = envItems[index].copy(state = state)
        _uiState.value = uiState.copy(envItems = envItems.toList())
    }

    private suspend fun runAndValidate(context: Context, viewModel: GuideViewModel, run: suspend () -> Unit) {
        run()
        val uiState by uiState
        var allValidated = true
        uiState.envItems.forEach {
            if (it.state != TokenState.Succeed) allValidated = false
        }
        if (allValidated) {
            _uiState.value = uiState.copy(allValidated = true)
            context.saveAppVersionName()
            viewModel.toUiState(
                GuideUiState.Env(
                    title = context.getString(R.string.environment_detection),
                    fabIcon = Icons.Rounded.Check,
                    onFabClick = { _ ->
                        context.startActivity(Intent(context, MainActivity::class.java))
                        (context as ComponentActivity).finish()
                    }
                )
            )
        }
    }
}
