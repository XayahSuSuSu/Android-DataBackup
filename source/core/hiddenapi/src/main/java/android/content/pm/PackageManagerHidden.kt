package android.content.pm

import android.annotation.SuppressLint
import android.os.PersistableBundle
import android.os.UserHandle
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
@Suppress("UNCHECKED_CAST")
object PackageManagerHidden {
    fun getInstalledPackagesAsUser(packageManager: PackageManager, flags: Int, userId: Int): List<PackageInfo> =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "getInstalledPackagesAsUser", flags, userId) as List<PackageInfo>

    fun getPackageInfoAsUser(packageManager: PackageManager, packageName: String, flags: Int, userId: Int): PackageInfo =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "getPackageInfoAsUser", packageName, flags, userId) as PackageInfo

    fun grantRuntimePermission(packageManager: PackageManager, packageName: String, permName: String, user: UserHandle) =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "grantRuntimePermission", packageName, permName, user)

    fun revokeRuntimePermission(packageManager: PackageManager, packageName: String, permName: String, user: UserHandle) =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "revokeRuntimePermission", packageName, permName, user)

    fun getPermissionFlags(packageManager: PackageManager, packageName: String, permName: String, user: UserHandle): Int =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "getPermissionFlags", permName, packageName, user) as Int

    fun updatePermissionFlags(packageManager: PackageManager, packageName: String, permName: String, user: UserHandle, flagMask: Int, flagValues: Int) =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "updatePermissionFlags", permName, packageName, flagMask, flagValues, user)

    fun setPackagesSuspended(packageManager: PackageManager, packageNames: Array<String>, suspended: Boolean, appExtras: PersistableBundle?, launcherExtras: PersistableBundle?, dialogInfo: Any?) =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "setPackagesSuspended", packageNames, suspended, appExtras, launcherExtras, dialogInfo)

    fun isPackageSuspended(packageManager: PackageManager, packageName: String): Boolean =
        HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "isPackageSuspended", packageName) as Boolean
}
