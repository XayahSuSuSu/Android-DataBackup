package com.xayah.feature.main.configurations

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.LabelsRepo
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.model.CompressionType
import com.xayah.core.model.Configurations
import com.xayah.core.model.OpType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.LabelAppCrossRefEntity
import com.xayah.core.model.database.LabelEntity
import com.xayah.core.model.database.LabelFileCrossRefEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStats
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageExtraInfo
import com.xayah.core.model.database.PackageIndexInfo
import com.xayah.core.model.database.PackageInfo
import com.xayah.core.model.database.PackageStorageStats
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.CommonBackupUtil
import com.xayah.core.ui.component.DialogState
import com.xayah.core.ui.component.select
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.model.DialogCheckBoxItem
import com.xayah.core.ui.util.joinOf
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
    val fileSelected: Boolean,
    val labelSelected: Boolean,
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
    private val mediaRepo: MediaRepository,
    private val labelsRepo: LabelsRepo,
    private val commonBackupUtil: CommonBackupUtil,
    private val pathUtil: PathUtil,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        selectedCount = 4,
        blacklistSelected = true,
        cloudSelected = true,
        fileSelected = true,
        labelSelected = true,
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Export -> {
                rootService.mkdirs(pathUtil.getLocalBackupConfigsDir())
                val result = commonBackupUtil.backupConfigs(pathUtil.getLocalBackupConfigsDir())
                emitEffect(IndexUiEffect.DismissSnackbar)
                emitEffect(IndexUiEffect.ShowSnackbar(type = if (result.isSuccess) SnackbarType.Success else SnackbarType.Error, message = result.outString, duration = SnackbarDuration.Short))
            }

            is IndexUiIntent.Import -> {
                val src = "${pathUtil.getLocalBackupConfigsDir()}/$ConfigsConfigurationsName"
                if (rootService.exists(src)) {
                    val config = rootService.readJson<Configurations>(src)
                    val items = mutableListOf<DialogCheckBoxItem<String>>()
                    if (config != null) {
                        runCatching {
                            val appsCount = config.blacklist.apps.size
                            val filesCount = config.blacklist.files.size
                            if (appsCount + filesCount != 0) {
                                items.add(
                                    DialogCheckBoxItem(
                                        enum = ConstantUtil.CONFIGURATIONS_KEY_BLACKLIST,
                                        title = joinOf(
                                            context.getString(R.string.blacklist),
                                            " (${appsCount + filesCount})",
                                        )
                                    )
                                )
                            }
                        }.withLog()
                        runCatching {
                            if (config.cloud.isNotEmpty()) {
                                items.add(
                                    DialogCheckBoxItem(
                                        enum = ConstantUtil.CONFIGURATIONS_KEY_CLOUD,
                                        title = joinOf(context.getString(R.string.cloud), " (${config.cloud.size})")
                                    )
                                )
                            }
                        }.withLog()
                        runCatching {
                            if (config.file.isNotEmpty()) {
                                items.add(
                                    DialogCheckBoxItem(
                                        enum = ConstantUtil.CONFIGURATIONS_KEY_FILE,
                                        title = joinOf(
                                            context.getString(R.string.files),
                                            " (${config.file.size})",
                                        )
                                    )
                                )
                            }
                        }.withLog()
                        runCatching {
                            var size = 0
                            if (config.labels.isNotEmpty()) {
                                size += config.labels.size
                            }
                            if (config.labelAppRefs.isNotEmpty()) {
                                size += config.labelAppRefs.size
                            }
                            if (config.labelFileRefs.isNotEmpty()) {
                                size += config.labelFileRefs.size
                            }

                            if (size != 0) {
                                items.add(
                                    DialogCheckBoxItem(
                                        enum = ConstantUtil.CONFIGURATIONS_KEY_LABEL,
                                        title = joinOf(
                                            context.getString(R.string.labels),
                                            " ($size)",
                                        )
                                    )
                                )
                            }
                        }.withLog()
                    }

                    if (items.isEmpty()) {
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        emitEffect(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = context.getString(R.string.config_file_may_be_broken), duration = SnackbarDuration.Short))
                    } else {
                        val (s, selection) = intent.dialogState.select(
                            title = context.getString(R.string._import),
                            def = items.map { true },
                            items = items
                        )
                        if (s.isConfirm) {
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
                                                        packageInfo = PackageInfo(label = "", versionName = "", versionCode = 0L, flags = 0, firstInstallTime = 0L, lastUpdateTime = 0L),
                                                        extraInfo = PackageExtraInfo(
                                                            uid = 0,
                                                            hasKeystore = false,
                                                            permissions = listOf(),
                                                            ssaid = "",
                                                            lastBackupTime = 0L,
                                                            blocked = true,
                                                            activated = false,
                                                            firstUpdated = true,
                                                            enabled = true,
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

                                            if (config?.blacklist?.files != null) {
                                                mediaRepo.clearBlocked()
                                                config.blacklist.files.forEach {
                                                    val media = mediaRepo.query(name = it.name, opType = OpType.BACKUP) ?: MediaEntity(
                                                        id = 0,
                                                        indexInfo = MediaIndexInfo(
                                                            opType = OpType.BACKUP,
                                                            name = it.name,
                                                            compressionType = CompressionType.TAR,
                                                            preserveId = 0,
                                                            cloud = "",
                                                            backupDir = ""
                                                        ),
                                                        mediaInfo = MediaInfo(
                                                            path = it.path,
                                                            dataBytes = 0,
                                                            displayBytes = 0,
                                                        ),
                                                        extraInfo = MediaExtraInfo(
                                                            lastBackupTime = 0,
                                                            blocked = true,
                                                            activated = false,
                                                            existed = false,
                                                        ),
                                                    )
                                                    media.extraInfo.blocked = true
                                                    media.extraInfo.activated = false
                                                    mediaRepo.upsert(media)
                                                }
                                            }
                                        }

                                        ConstantUtil.CONFIGURATIONS_KEY_CLOUD -> {
                                            if (config?.cloud != null) {
                                                cloudRepo.upsert(config.cloud)
                                            }
                                        }

                                        ConstantUtil.CONFIGURATIONS_KEY_FILE -> {
                                            if (config?.file != null) {
                                                mediaRepo.addMedia(config.file.map { it.path })
                                            }
                                        }

                                        ConstantUtil.CONFIGURATIONS_KEY_LABEL -> {
                                            if (config?.labels != null) {
                                                labelsRepo.addLabels(config.labels)
                                            }
                                            if (config?.labelAppRefs != null) {
                                                labelsRepo.addLabelAppCrossRefs(config.labelAppRefs)
                                            }
                                            if (config?.labelFileRefs != null) {
                                                labelsRepo.addLabelFileCrossRefs(config.labelFileRefs)
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

    private val _blockedFiles: Flow<List<MediaEntity>> = mediaRepo.queryFlow(opType = OpType.BACKUP, blocked = true).flowOnIO()
    val blockedFilesState: StateFlow<List<MediaEntity>> = _blockedFiles.stateInScope(listOf())

    private val _accounts: Flow<List<CloudEntity>> = cloudRepo.clouds.flowOnIO()
    val accounts: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())

    private val _files: Flow<List<MediaEntity>> = mediaRepo.queryFlow(opType = OpType.BACKUP, blocked = false).flowOnIO()
    val files: StateFlow<List<MediaEntity>> = _files.stateInScope(listOf())

    private val _labels: Flow<List<LabelEntity>> = labelsRepo.getLabelsFlow().flowOnIO()
    private val _labelAppRefs: Flow<List<LabelAppCrossRefEntity>> = labelsRepo.getAppRefsFlow().flowOnIO()
    private val _labelFileRefs: Flow<List<LabelFileCrossRefEntity>> = labelsRepo.getFileRefsFlow().flowOnIO()
    val labels: StateFlow<List<LabelEntity>> = _labels.stateInScope(listOf())
    val labelAppRefs: StateFlow<List<LabelAppCrossRefEntity>> = _labelAppRefs.stateInScope(listOf())
    val labelFileRefs: StateFlow<List<LabelFileCrossRefEntity>> = _labelFileRefs.stateInScope(listOf())
}
