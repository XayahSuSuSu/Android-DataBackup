package android.content.pm

import android.annotation.SuppressLint
import android.os.PersistableBundle
import android.os.UserHandle
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("NewApi", "PrivateApi")
class PackageManagerHidden {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun getInstalledPackagesAsUser(packageManager: PackageManager, flags: Int, userId: Int): List<PackageInfo> {
            return HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "getInstalledPackagesAsUser", flags, userId) as List<PackageInfo>
        }

        fun getPackageInfoAsUser(packageManager: PackageManager, packageName: String, flags: Int, userId: Int): PackageInfo {
            return HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "getPackageInfoAsUser", packageName, flags, userId) as PackageInfo
        }

        fun grantRuntimePermission(packageManager: PackageManager, packageName: String, permName: String, user: UserHandle) {
            HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "grantRuntimePermission", packageName, permName, user)
        }

        fun setPackagesSuspended(packageManager: PackageManager, packageNames: Array<String>, suspended: Boolean, appExtras: PersistableBundle?, launcherExtras: PersistableBundle?, dialogInfo: Any?) {
            HiddenApiBypass.invoke(Class.forName("android.content.pm.PackageManager"), packageManager, "setPackagesSuspended", packageNames, suspended, appExtras, launcherExtras, dialogInfo)
        }
    }
}