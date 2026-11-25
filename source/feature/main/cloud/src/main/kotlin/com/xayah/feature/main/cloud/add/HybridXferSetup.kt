package com.xayah.feature.main.cloud.add

import android.os.StatFs
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.hfx.NetInterfaceDiscoverer
import com.xayah.core.model.database.HybridXferExtra
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.feature.main.cloud.AccountSetupScaffold
import com.xayah.feature.main.cloud.R
import com.xayah.feature.main.cloud.SetupTextField
import kotlin.math.roundToInt

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageHybridXferSetup() {
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val savedExtra = uiState.cloudEntity?.getExtraEntity<HybridXferExtra>()
    var name by rememberSaveable { mutableStateOf(uiState.currentName.ifEmpty { "PC-HybridXfer" }) }
    var remote by rememberSaveable(uiState.cloudEntity) { mutableStateOf(uiState.cloudEntity?.remote ?: "/DataBackup") }
    var port by rememberSaveable(uiState.cloudEntity) { mutableStateOf((savedExtra?.port ?: 5740).toString()) }
    var maxBufferMb by rememberSaveable(uiState.cloudEntity) { mutableStateOf((savedExtra?.maxBufferMb ?: 1024).toString()) }

    val availableStorageText = remember {
        runCatching {
            val availBytes = StatFs(context.cacheDir.absolutePath).availableBytes
            val gb = availBytes / (1024.0 * 1024.0 * 1024.0)
            val mb = availBytes / (1024L * 1024L)
            String.format("当前手机剩余存储: %.2f GB (%d MB)", gb, mb)
        }.getOrDefault("")
    }

    val chunkOptions = listOf(32, 64, 128)
    var chunkIndex by rememberSaveable(uiState.cloudEntity) {
        val savedChunk = savedExtra?.chunkSizeMb ?: 64
        mutableIntStateOf(chunkOptions.indexOf(savedChunk).takeIf { it >= 0 } ?: 1)
    }

    // 提速优化自定义开关与滑块（高自由度用户控制）
    var continuousStream by rememberSaveable(uiState.cloudEntity) {
        mutableStateOf(savedExtra?.continuousStream ?: true)
    }
    var usbParallelChannels by rememberSaveable(uiState.cloudEntity) {
        mutableIntStateOf((savedExtra?.usbParallelChannels ?: 2).coerceIn(1, 24))
    }
    val sockBufOptions = listOf(1, 2, 4, 8)
    var sockBufIndex by rememberSaveable(uiState.cloudEntity) {
        val saved = savedExtra?.socketBufferMb ?: 4
        mutableIntStateOf(sockBufOptions.indexOf(saved).takeIf { it >= 0 } ?: 2)
    }
    val ramBufPresets = listOf(64, 128, 256, 512, 1024)
    var ramBufferMb by rememberSaveable(uiState.cloudEntity) {
        mutableIntStateOf((savedExtra?.localBufferCount ?: 64).coerceIn(32, 1024))
    }
    var inlineCrc32 by rememberSaveable(uiState.cloudEntity) {
        mutableStateOf(savedExtra?.inlineCrc32 ?: true)
    }

    var discoveredInterfaces by remember { mutableStateOf(NetInterfaceDiscoverer.discoverAll()) }
    var selectedInterfaces by remember(uiState.cloudEntity) {
        mutableStateOf(
            savedExtra?.enabledInterfaces?.toSet()
                ?: discoveredInterfaces.filter { it.defaultEnabled }.map { it.name }.toSet()
        )
    }

    val allFilled = name.isNotEmpty() &&
        (port.toIntOrNull() ?: 0) in 1..65535 &&
        (maxBufferMb.toIntOrNull() ?: 0) >= 64 &&
        selectedInterfaces.isNotEmpty()

    suspend fun syncEntity() {
        viewModel.updateHybridXferEntity(
            name = name,
            remote = remote,
            port = port,
            enabledInterfaces = selectedInterfaces.toList(),
            maxBufferMb = maxBufferMb,
            chunkSizeMb = chunkOptions[chunkIndex],
            continuousStream = continuousStream,
            usbParallelChannels = usbParallelChannels,
            socketBufferMb = sockBufOptions[sockBufIndex],
            localBufferCount = ramBufferMb,
            inlineCrc32 = inlineCrc32,
        )
    }

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Initialize)
    }

    AccountSetupScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.hybridxfer_setup),
        actions = {
            TextButton(
                enabled = allFilled && uiState.isProcessing.not(),
                onClick = {
                    viewModel.launchOnIO {
                        syncEntity()
                        viewModel.emitIntent(IndexUiIntent.TestConnection)
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.test_connection))
            }

            Button(
                enabled = allFilled && remote.isNotEmpty() && uiState.isProcessing.not(),
                onClick = {
                    viewModel.launchOnIO {
                        syncEntity()
                        viewModel.emitIntent(IndexUiIntent.CreateAccount(navController = navController))
                    }
                }
            ) {
                Text(text = stringResource(id = R.string._continue))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Title(
                enabled = uiState.isProcessing.not(),
                title = stringResource(id = R.string.server),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
            ) {
                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.currentName.isEmpty() && uiState.isProcessing.not(),
                    value = name,
                    leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_badge),
                    onValueChange = { name = it },
                    label = stringResource(id = R.string.name)
                )

                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    value = port,
                    leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_lan),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { port = it.filter { ch -> ch.isDigit() } },
                    label = stringResource(id = R.string.port)
                )
            }

            Title(
                enabled = uiState.isProcessing.not(),
                title = stringResource(id = R.string.hybridxfer_channels),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
            ) {
                discoveredInterfaces.forEach { iface ->
                    val checked = selectedInterfaces.contains(iface.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(SizeTokens.Level24),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "${iface.name} (${iface.ipAddress})")
                        Checkbox(
                            enabled = uiState.isProcessing.not(),
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectedInterfaces = if (isChecked) {
                                    selectedInterfaces + iface.name
                                } else {
                                    selectedInterfaces - iface.name
                                }
                            }
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    onClick = { discoveredInterfaces = NetInterfaceDiscoverer.discoverAll() }
                ) {
                    Text(text = stringResource(id = R.string.hybridxfer_refresh_channels))
                }
            }

            // 极速性能调优面板（用户自定义开关与滑块）
            Title(
                enabled = uiState.isProcessing.not(),
                title = stringResource(id = R.string.hybridxfer_perf_settings),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
            ) {
                // 开关 1：极速连续流（零落盘直推）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.hybridxfer_continuous_stream))
                        Text(
                            text = stringResource(id = R.string.hybridxfer_continuous_stream_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        enabled = uiState.isProcessing.not(),
                        checked = continuousStream,
                        onCheckedChange = { continuousStream = it },
                    )
                }

                // 滑块：USB ADB 并行通道数 (1~24) + USB/Wi-Fi 倍率计算提示
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24)
                ) {
                    Text(text = "${stringResource(id = R.string.hybridxfer_usb_parallel)}: $usbParallelChannels")
                    Text(
                        text = stringResource(id = R.string.hybridxfer_usb_parallel_desc),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Slider(
                        enabled = uiState.isProcessing.not(),
                        value = usbParallelChannels.toFloat(),
                        onValueChange = { usbParallelChannels = it.roundToInt().coerceIn(1, 24) },
                        valueRange = 1f..24f,
                        steps = 22,
                    )
                }

                // 分段：Socket 缓冲区大小 (1/2/4/8 MB)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
                ) {
                    Text(text = stringResource(id = R.string.hybridxfer_socket_buffer_mb))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sockBufOptions.forEachIndexed { idx, mb ->
                            SegmentedButton(
                                enabled = uiState.isProcessing.not(),
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = sockBufOptions.size),
                                onClick = { sockBufIndex = idx },
                                selected = idx == sockBufIndex,
                            ) {
                                Text(text = "${mb} MB")
                            }
                        }
                    }
                }

                // 分段 + 滑块：手机内存传输缓冲池大小 (32 MB ~ 1024 MB / 1 GB)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
                ) {
                    val ramDisplay = if (ramBufferMb >= 1024) "1 GB (1024 MB)" else "$ramBufferMb MB"
                    Text(text = "${stringResource(id = R.string.hybridxfer_ram_buffer_mb)}: $ramDisplay")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ramBufPresets.forEachIndexed { idx, mb ->
                            val label = if (mb >= 1024) "1G" else "${mb}M"
                            SegmentedButton(
                                enabled = uiState.isProcessing.not(),
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = ramBufPresets.size),
                                onClick = { ramBufferMb = mb },
                                selected = ramBufferMb == mb,
                            ) {
                                Text(text = label)
                            }
                        }
                    }
                    Slider(
                        enabled = uiState.isProcessing.not(),
                        value = ramBufferMb.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 32f).roundToInt() * 32).coerceIn(32, 1024)
                            ramBufferMb = snapped
                        },
                        valueRange = 32f..1024f,
                        steps = 30,
                    )
                }

                // 开关 2：每 1MB 块内联 CRC32 校验
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.hybridxfer_inline_crc32))
                        Text(
                            text = stringResource(id = R.string.hybridxfer_inline_crc32_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        enabled = uiState.isProcessing.not(),
                        checked = inlineCrc32,
                        onCheckedChange = { inlineCrc32 = it },
                    )
                }
            }

            // 最大磁盘缓冲池硬上限（始终展示，严防闪存已满时报错）
            Title(
                enabled = uiState.isProcessing.not(),
                title = stringResource(id = R.string.hybridxfer_buffer_settings),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                if (availableStorageText.isNotEmpty()) {
                    Text(
                        modifier = Modifier.paddingHorizontal(SizeTokens.Level24),
                        text = availableStorageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemedColorSchemeKeyTokens.Primary.value,
                    )
                }
                Text(
                    modifier = Modifier.paddingHorizontal(SizeTokens.Level24),
                    text = stringResource(id = R.string.hybridxfer_max_buffer_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
                SetupTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingHorizontal(SizeTokens.Level24),
                    enabled = uiState.isProcessing.not(),
                    value = maxBufferMb,
                    leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_deployed_code),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { maxBufferMb = it.filter { ch -> ch.isDigit() } },
                    label = stringResource(id = R.string.hybridxfer_max_buffer_mb)
                )

                if (!continuousStream) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paddingHorizontal(SizeTokens.Level24)
                    ) {
                        chunkOptions.forEachIndexed { idx, mb ->
                            SegmentedButton(
                                enabled = uiState.isProcessing.not(),
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = chunkOptions.size),
                                onClick = { chunkIndex = idx },
                                selected = idx == chunkIndex,
                            ) {
                                Text(text = "${mb} MB")
                            }
                        }
                    }
                }
            }

            Title(
                enabled = uiState.isProcessing.not(),
                title = stringResource(id = R.string.advanced),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
            ) {
                Clickable(
                    enabled = allFilled && uiState.isProcessing.not(),
                    title = stringResource(id = R.string.remote_path),
                    value = remote.ifEmpty { stringResource(id = R.string.not_selected) },
                    desc = stringResource(id = R.string.remote_path_desc),
                ) {
                    viewModel.launchOnIO {
                        syncEntity()
                        viewModel.emitIntent(IndexUiIntent.SetRemotePath(context = context))
                    }
                }

                if (uiState.currentName.isNotEmpty()) {
                    TextButton(
                        modifier = Modifier
                            .paddingHorizontal(SizeTokens.Level24)
                            .fillMaxWidth(),
                        enabled = uiState.isProcessing.not(),
                        onClick = {
                            viewModel.launchOnIO {
                                if (dialogState.confirm(
                                        title = context.getString(R.string.delete_account),
                                        text = context.getString(R.string.delete_account_desc)
                                    )
                                ) {
                                    viewModel.emitIntent(IndexUiIntent.DeleteAccount(navController = navController))
                                }
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.delete_account),
                            color = ThemedColorSchemeKeyTokens.Error.value.withState(uiState.isProcessing.not())
                        )
                    }
                }
            }
        }
    }
}
