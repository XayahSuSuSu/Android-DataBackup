package com.xayah.core.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.core.content.pm.PermissionInfoCompat
import com.xayah.core.model.database.PackagePermission


object PermissionUtil {
    fun getPermission(packageManager: PackageManager, packageInfo: PackageInfo): List<PackagePermission> = run {
        val permissions = mutableListOf<PackagePermission>()
        val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: listOf()
        val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags?.toList() ?: listOf()
        requestedPermissions.forEachIndexed { i, name ->
            runCatching {
                val permissionInfo = packageManager.getPermissionInfo(name, 0)
                val protection = PermissionInfoCompat.getProtection(permissionInfo)
                val protectionFlags = PermissionInfoCompat.getProtectionFlags(permissionInfo)
                val isGranted = (requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (protection == PermissionInfo.PROTECTION_DANGEROUS || (protectionFlags and PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0) {
                    permissions.add(PackagePermission(name, isGranted))
                }
            }
        }
        permissions
    }
}
