package com.xayah.libnative

object NativeLib {
    external fun calculateTreeSize(path: String): Long
    external fun getUidGid(path: String): IntArray

    /**
     * Restores the SELinux context of the given path.
     *
     * @param flags Bitwise OR of SELINUX_ANDROID_RESTORECON_* flags.
     * @return 0 on success, -1 on failure. Success does not necessarily mean the context was changed.
     * @throws UnsupportedOperationException if the native symbol cannot be resolved.
     */
    external fun selinuxAndroidRestorecon(path: String, flags: Int): Int

    /**
     * Restores SELinux contexts for an app data directory using its seinfo and UID.
     *
     * @param seinfo SELinux information associated with the app.
     * @param uid Full Linux UID, including the Android user ID component; not just the appId.
     * @param flags Bitwise OR of SELINUX_ANDROID_RESTORECON_* flags.
     * @return 0 on success, -1 on failure. Success does not necessarily mean the contexts were changed.
     * @throws UnsupportedOperationException if the native symbol cannot be resolved.
     */
    external fun selinuxAndroidRestoreconPkgdir(path: String, seinfo: String, uid: Int, flags: Int): Int
}
