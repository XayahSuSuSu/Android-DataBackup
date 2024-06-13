package com.xayah.feature.main.configurations

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.model.BlacklistAppItem
import com.xayah.core.model.CompressionType
import com.xayah.core.model.Configurations
import com.xayah.core.model.ConfigurationsBlacklist
import com.xayah.core.model.OpType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStats
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageExtraInfo
import com.xayah.core.model.database.PackageIndexInfo
import com.xayah.core.model.database.PackageInfo
import com.xayah.core.model.database.PackageStorageStats
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.component.DialogState
import com.xayah.core.ui.component.select
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.model.DialogCheckBoxItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.ConfigsConfigurationsName
import com.xayah.core.util.PathUtil
import com.xayah.core.util.withLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val selectedCount: Int,
    val blacklistSelected: Boolean,
    val cloudSelected: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Export : IndexUiIntent()
    data class Import(val dialogState: DialogState) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val packageRepo: PackageRepository,
    private val cloudRepo: CloudRepository,
    private val pathUtil: PathUtil,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(selectedCount = 2, blacklistSelected = true, cloudSelected = true)) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Export -> {
                val config = Configurations(
                    blacklist = ConfigurationsBlacklist(apps = listOf()),
                    cloud = listOf()
                )
                if (state.blacklistSelected) {
                    val apps = mutableListOf<BlacklistAppItem>()
                    blockedPackagesState.value.forEach {
                        apps.add(BlacklistAppItem(it.packageName, it.userId))
                    }
                    config.blacklist.apps = apps
                }
                if (state.cloudSelected) {
                    config.cloud = accounts.value
                }
                rootService.mkdirs(pathUtil.getLocalBackupConfigsDir())
                val dst = "${pathUtil.getLocalBackupConfigsDir()}/$ConfigsConfigurationsName"
                rootService.writeJson(data = config, dst = dst).apply {
                    if (isSuccess) {
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        emitEffect(IndexUiEffect.ShowSnackbar(type = SnackbarType.Success, message = "${context.getString(R.string.exported)}: $dst", duration = SnackbarDuration.Short))
                    } else {
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        emitEffect(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = context.getString(R.string.failed), duration = SnackbarDuration.Short))
                    }
                }
            }

            is IndexUiIntent.Import -> {
                val src = "${pathUtil.getLocalBackupConfigsDir()}/$ConfigsConfigurationsName"
                if (rootService.exists(src)) {
                    val config = rootService.readJson<Configurations>(src)
                    val items = mutableListOf<DialogCheckBoxItem<String>>()
                    runCatching {
                        var count = 0
                        count += config?.blacklist?.apps?.size ?: 0
                        if (count != 0) {
                            items.add(
                                DialogCheckBoxItem(
                                    enum = ConstantUtil.CONFIGURATIONS_KEY_BLACKLIST,
                                    title = StringResourceToken.fromStringArgs(
                                        StringResourceToken.fromStringId(R.string.blacklist),
                                        StringResourceToken.fromString(" (${count})"),
                                    )
                                )
                            )
                        }
                    }.withLog()
                    runCatching {
                        if (config?.cloud != null) {
                            if (config.cloud.isNotEmpty()) {
                                items.add(
                                    DialogCheckBoxItem(
                                        enum = ConstantUtil.CONFIGURATIONS_KEY_CLOUD,
                                        title = StringResourceToken.fromStringArgs(
                                            StringResourceToken.fromStringId(R.string.cloud),
                                            StringResourceToken.fromString(" (${config.cloud.size})"),
                                        )
                                    )
                                )
                            }
                        }
                    }.withLog()
                    if (items.isEmpty()) {
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        emitEffect(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = context.getString(R.string.config_file_may_be_broken), duration = SnackbarDuration.Short))
                    } else {
                        val (s, selection) = intent.dialogState.select(
                            title = StringResourceToken.fromStringId(R.string._import),
                            def = items.map { true },
                            items = items
                        )
                        if (s) {
                            items.forEachIndexed { i, item ->
                                if (selection[i]) {
                                    when (item.enum) {
                                        ConstantUtil.CONFIGURATIONS_KEY_BLACKLIST -> {
                                            if (config?.blacklist?.apps != null) {
                                                packageRepo.clearBlocked()
                                                config.blacklist.apps.forEach {
                                                    val pkg = packageRepo.getPackage(packageName = it.packageName, opType = OpType.BACKUP, userId = it.userId) ?: PackageEntity(
                                                        id = 0,
                                                        indexInfo = PackageIndexInfo(
                                                            opType = OpType.BACKUP,
                                                            packageName = it.packageName,
                                                            userId = it.userId,
                                                            compressionType = CompressionType.ZSTD,
                                                            preserveId = 0,
                                                            cloud = "",
                                                            backupDir = ""
                                                        ),
                                                        packageInfo = PackageInfo(label = "", versionName = "", versionCode = 0L, flags = 0, firstInstallTime = 0L),
                                                        extraInfo = PackageExtraInfo(
                                                            uid = 0,
                                                            labels = listOf(),
                                                            hasKeystore = false,
                                                            permissions = listOf(),
                                                            ssaid = "",
                                                            blocked = true,
                                                            activated = false,
                                                            existed = false
                                                        ),
                                                        dataStats = PackageDataStats(),
                                                        dataStates = PackageDataStates(),
                                                        storageStats = PackageStorageStats(),
                                                        displayStats = PackageDataStats(),
                                                    )
                                                    pkg.extraInfo.blocked = true
                                                    pkg.extraInfo.activated = false
                                                    packageRepo.upsert(pkg)
                                                }
                                            }
                                        }

                                        ConstantUtil.CONFIGURATIONS_KEY_CLOUD -> {
                                            if (config?.cloud != null) {
                                                cloudRepo.upsert(config.cloud)
                                            }
                                        }
                                    }
                                }

                            }

                            emitEffect(IndexUiEffect.DismissSnackbar)
                            emitEffect(IndexUiEffect.ShowSnackbar(type = SnackbarType.Success, message = context.getString(R.string.imported), duration = SnackbarDuration.Short))
                        }
                    }
                } else {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffect(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = "${context.getString(R.string.not_exist)}: $src", duration = SnackbarDuration.Short))
                }
            }
        }
    }

    private val _blockedPackages: Flow<List<PackageEntity>> = packageRepo.queryPackagesFlow(opType = OpType.BACKUP, blocked = true).flowOnIO()
    val blockedPackagesState: StateFlow<List<PackageEntity>> = _blockedPackages.stateInScope(listOf())

    private val _accounts: Flow<List<CloudEntity>> = cloudRepo.clouds.flowOnIO()
    val accounts: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())
}
