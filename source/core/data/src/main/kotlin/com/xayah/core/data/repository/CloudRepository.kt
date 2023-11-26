package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.google.gson.reflect.TypeToken
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.database.model.AccountMap
import com.xayah.core.database.model.CloudAccountEntity
import com.xayah.core.database.model.CloudEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.Rclone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class CloudRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val cloudDao: CloudDao,
    private val gsonUtil: GsonUtil,
    private val pathUtil: PathUtil,
) {
    private fun log(msg: () -> String) = LogUtil.log { "CloudRepository" to msg() }
    fun getString(@StringRes resId: Int) = context.getString(resId)
    suspend fun upsertCloud(item: CloudEntity) = cloudDao.upsertCloud(item)
    suspend fun queryCloudByName(name: String) = cloudDao.queryCloudByName(name)

    val clouds = cloudDao.queryActiveCloudsFlow().distinctUntilChanged()

    suspend fun update() {
        Rclone.Config.dump().also { result ->
            val type = object : TypeToken<AccountMap>() {}.type
            val accountMap = runCatching { gsonUtil.fromJson<AccountMap>(result.outString, type) }.onFailure {
                val msg = it.message
                if (msg != null)
                    log { msg }
            }.getOrElse { AccountMap() }
            // Inactivate all cloud entities.
            cloudDao.updateActive(false)
            cloudDao.upsertAccount(accountMap.map { (key, value) ->
                CloudAccountEntity(
                    name = key,
                    account = value,
                    active = true,
                )
            })
        }
    }

    suspend fun delete(entity: CloudEntity) = Rclone.Config.delete(entity.name).also { result ->
        if (result.isSuccess) {
            cloudDao.deleteCloud(entity)
        }
    }

    fun getTmpMountPath(name: String) = pathUtil.getTmpMountPath(name)

    suspend fun mountTmp(name: String, tmpMountPath: String) = run {
        rootService.mkdirs(tmpMountPath)
        Rclone.mount(
            src = "${name}:",
            dst = tmpMountPath,
            "--allow-non-empty",
            "--allow-other",
            "--vfs-cache-mode off",
            "--read-only",
        )
    }

    suspend fun unmountTmp(tmpMountPath: String) = run {
        BaseUtil.kill("rclone")
        BaseUtil.umount(tmpMountPath)
        rootService.deleteRecursively(tmpMountPath)
    }
}
