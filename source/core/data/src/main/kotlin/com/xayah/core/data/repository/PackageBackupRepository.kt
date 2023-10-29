package com.xayah.core.data.repository

import com.xayah.core.database.dao.PackageBackupEntireDao
import javax.inject.Inject

class PackageBackupRepository @Inject constructor(
    private val packageBackupDao: PackageBackupEntireDao,
) {
    fun countSelectedAPKs() = packageBackupDao.countSelectedAPKs()
    fun countSelectedData() = packageBackupDao.countSelectedData()
}
