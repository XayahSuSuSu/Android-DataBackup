package com.xayah.core.data.repository

import com.xayah.librootservice.service.RemoteRootService
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val rootService: RemoteRootService,
) {
    suspend fun getUsers() = runCatching { rootService.getUsers() }.getOrDefault(listOf())
}
