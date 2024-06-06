package android.content.pm;

import android.os.UserHandle;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/content/pm/PackageManager.java">PackageManager.java</a>
 */
@RefineAs(PackageManager.class)
public class PackageManagerHidden {
    public List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        throw new RuntimeException("Stub!");
    }

    public PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId) {
        throw new RuntimeException("Stub!");
    }

    public void grantRuntimePermission(String packageName, String permissionName, UserHandle user) {
        throw new RuntimeException("Stub!");
    }

    public void revokeRuntimePermission(String packageName, String permissionName, UserHandle user) {
        throw new RuntimeException("Stub!");
    }

    public int getPermissionFlags(String permissionName, String packageName, UserHandle user) {
        throw new RuntimeException("Stub!");
    }

    public void updatePermissionFlags(String permissionName, String packageName, int flagMask, int flagValues, UserHandle user) {
        throw new RuntimeException("Stub!");
    }
}
