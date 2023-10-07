package com.xayah.databackup.ui.activity.guide.page.env

import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.guide.page.GuideUiState
import com.xayah.databackup.ui.activity.guide.page.GuideViewModel
import com.xayah.databackup.ui.activity.main.MainActivity
import com.xayah.databackup.ui.component.EnvCard
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.State
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.saveAppVersionName
import com.xayah.librootservice.util.ExceptionUtil.tryOnScope
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ExperimentalMaterial3Api
@Composable
fun PageEnv(viewModel: GuideViewModel) {
    val context = LocalContext.current
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val scope = rememberCoroutineScope()
    val mutex = remember {
        Mutex()
    }
    val contents = listOf(
        stringResource(id = R.string.grant_root_access),
        stringResource(id = R.string.release_prebuilt_binaries),
        stringResource(id = R.string.abi_validation),
    )
    val states = remember { mutableStateOf(listOf<State>(State.Loading, State.Loading, State.Loading)) }
    val runAndValidate: suspend (run: suspend () -> Unit) -> Unit = { run ->
        run()
        var allValidated = true
        states.value.forEach {
            if (it != State.Succeed) allValidated = false
        }
        if (allValidated) {
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

    val onClicks = listOf<suspend () -> Unit>(
        {
            if (states.value[0] != State.Succeed)
                runAndValidate {
                    tryOnScope {
                        withIOContext {
                            val statesList = states.value.toMutableList()
                            statesList[0] = if (Shell.getShell().isRoot) State.Succeed else State.Failed
                            states.value = statesList.toList()

                            // Kill daemon
                            PreparationUtil.killDaemon(context)
                        }
                    }
                }
        },
        {
            if (states.value[1] != State.Succeed)
                runAndValidate {
                    withIOContext {
                        val statesList = states.value.toMutableList()
                        statesList[1] = if (EnvUtil.releaseBin(context)) State.Succeed else State.Failed
                        states.value = statesList.toList()
                    }
                }
        },
        {
            if (states.value[2] != State.Succeed)
                runAndValidate {
                    withIOContext {
                        val statesList = states.value.toMutableList()
                        val buildABI = BuildConfig.FLAVOR_abi
                        val deviceABI = Build.SUPPORTED_ABIS.firstOrNull().toString()
                        if (buildABI == deviceABI) {
                            statesList[2] = State.Succeed
                        } else {
                            statesList[2] = State.Failed
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
                        states.value = statesList.toList()
                    }
                }
        },
    )

    LaunchedEffect(null) {
        viewModel.toUiState(
            GuideUiState.Env(
                title = context.getString(R.string.environment_detection),
                fabIcon = Icons.Rounded.ArrowForward,
                onFabClick = { _ ->
                    scope.launch {
                        for (i in onClicks) {
                            i()
                        }
                    }
                }
            )
        )
    }

    LazyColumn(
        modifier = Modifier.paddingTop(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
    ) {
        items(count = contents.size, key = { it }) {
            EnvCard(
                content = contents[it],
                state = states.value[it],
                onClick = {
                    scope.launch {
                        mutex.withLock {
                            onClicks[it]()
                        }
                    }
                })
        }
        item {
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingSmall))
        }
    }
}
