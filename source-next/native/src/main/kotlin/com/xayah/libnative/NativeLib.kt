package com.xayah.libnative

object NativeLib {
    external fun calculateTreeSize(path: String): Long
    external fun getUidGid(path: String): IntArray

    /**
     * Restores app data ownership, including cache, per-SDK and PCC GID rules.
     * Requires root, a stopped app and stable paths. Pass per-SDK roots, not SDK package containers.
     *
     * @param uid Full destination app, sandbox or PCC UID, including the Android user ID.
     * @param previousUid UID on restored files; pass uid for GID-only repair.
     * @return 0 on success, -1 on failure; partial changes may remain.
     */
    external fun chownAppDir(path: String, uid: Int, previousUid: Int): Int

    /**
     * Write the inode of a specific child file into the given xattr on the
     * parent directory. This allows you to find the child later, even if its
     * name is encrypted.
     */
    external fun writePathInode(parent: String, name: String, inodeXattr: String): Int

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
