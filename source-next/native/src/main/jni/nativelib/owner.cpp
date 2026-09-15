#include <android/api-level.h>
#include <android/log.h>
#include <jni.h>
#include <cerrno>
#include <cstring>
#include <fts.h>
#include <string>
#include <sys/stat.h>
#include <unistd.h>

// https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:system/core/libcutils/include/private/android_filesystem_config.h
#define AID_NOBODY 9999

#define AID_APP_START 10000 /* first app user */
#define AID_APP_END 19999   /* last app user */

#define AID_CACHE_GID_START 20000 /* start of gids for apps to mark cached data */

/* use the ranges below to determine whether a process is sdk sandbox */
#define AID_SDK_SANDBOX_PROCESS_START 20000 /* start of uids allocated to sdk sandbox processes */
#define AID_SDK_SANDBOX_PROCESS_END 29999   /* end of uids allocated to sdk sandbox processes */

/* use the ranges below to determine whether a process is a pcc component */
#define AID_PCC_COMPONENT_PROCESS_START \
    30000                                   /* start of uids allocated to pcc component processes */
#define AID_PCC_COMPONENT_PROCESS_END 39999 /* end of uids allocated to pcc component processes */

#define AID_PCC_CACHE_GID_START 60000 /* start of gids for pcc to mark cached data */

#define AID_USER_OFFSET 100000 /* offset for uid ranges for each user */

// https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/base/core/java/android/os/Build.java
#define ANDROID_API_O 26
#define ANDROID_API_TIRAMISU 33
#define ANDROID_API_CINNAMON_BUN 37

namespace {
    struct OwnerPolicy {
        gid_t root_gid;
        gid_t cache_gid;
    };

    // Ownership rules: app cache GIDs on Android 8+, SDK sandbox root GID
    // on Android 13+, and PCC cache GIDs on Android 17+.
    // uid is the full destination UID, including the Android user component;
    // pass the sandbox or PCC UID when restoring those directories.
    constexpr OwnerPolicy app_data_owner_policy(uid_t uid, int api_level) {
        const uid_t app_id = uid % AID_USER_OFFSET;
        if (api_level >= ANDROID_API_TIRAMISU && app_id >= AID_SDK_SANDBOX_PROCESS_START && app_id <= AID_SDK_SANDBOX_PROCESS_END) {
            // reconcileSdkData creates each per-SDK data directory with
            // owner sandboxUid and group AID_NOBODY. Its cache GID is the host
            // app's cache GID, which equals sandboxUid numerically.
            // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r84:frameworks/native/cmds/installd/InstalldNativeService.cpp;l=939
            return {AID_NOBODY, uid};
        }
        if (api_level >= ANDROID_API_CINNAMON_BUN && app_id >= AID_PCC_COMPONENT_PROCESS_START && app_id <= AID_PCC_COMPONENT_PROCESS_END) {
            // multiuser_get_cache_gid maps PCC IDs 30000–39999 to 60000–69999.
            // https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:system/core/libcutils/multiuser.cpp;l=51
            return {uid, uid - AID_PCC_COMPONENT_PROCESS_START + AID_PCC_CACHE_GID_START};
        }
        if (api_level >= ANDROID_API_O && app_id >= AID_APP_START && app_id <= AID_APP_END) {
            // multiuser_get_cache_gid maps app IDs 10000–19999 to 20000–29999.
            // https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:system/core/libcutils/multiuser.cpp;l=51
            return {uid, uid - AID_APP_START + AID_CACHE_GID_START};
        }
        // On Android 7, or when no dedicated GID rule applies,
        // both the root GID and cache GID default to the owner UID.
        return {uid, uid};
    }

    // https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/native/cmds/installd/InstalldNativeService.cpp;l=698
    int chown_app_dir(const char *path, uid_t uid, uid_t previous_uid) {
        if (!path || !*path || uid == static_cast<uid_t>(-1) || previous_uid == static_cast<uid_t>(-1)) {
            errno = EINVAL;
            return -1;
        }
        // Strip trailing slashes so lstat/FTS cannot dereference a root symlink.
        std::string root(path);
        while (root.size() > 1 && root.back() == '/') root.pop_back();
        struct stat root_stat{};
        if (lstat(root.c_str(), &root_stat) != 0) return -1;
        if (!S_ISDIR(root_stat.st_mode)) {
            errno = ENOTDIR;
            return -1;
        }
        const int api_level = android_get_device_api_level();
        if (api_level < 0) {
            errno = ENOSYS;
            return -1;
        }

        char *paths[] = {const_cast<char *>(root.c_str()), nullptr};
        FTS *tree = fts_open(paths, FTS_PHYSICAL | FTS_NOCHDIR | FTS_XDEV, nullptr);
        if (!tree) return -1;
        const OwnerPolicy policy = app_data_owner_policy(uid, api_level);
        int error = 0;
        for (;;) {
            errno = 0;
            FTSENT *entry = fts_read(tree);
            if (!entry) {
                error = errno;
                break;
            }
            if (entry->fts_info == FTS_ERR || entry->fts_info == FTS_DNR || entry->fts_info == FTS_NS) {
                error = entry->fts_errno ? entry->fts_errno : EIO;
                break;
            }
            if (entry->fts_info == FTS_DP) continue;
            if (entry->fts_info == FTS_DC) {
                error = ELOOP;
                break;
            }
            // FTS_XDEV prevents descent, but still yields the mount point itself.
            if (entry->fts_statp->st_dev != root_stat.st_dev) continue;
            entry->fts_number = entry->fts_level == 0 ? 0 : entry->fts_parent->fts_number;
            if (entry->fts_info == FTS_D && entry->fts_level == 1 &&
                (strcmp(entry->fts_name, "cache") == 0 || strcmp(entry->fts_name, "code_cache") == 0)) {
                entry->fts_number = 1;
            }
            // Preserve objects owned by another UID, but continue into directories.
            // Also process uid == previous_uid to repair GIDs after extraction.
            const bool is_root = entry->fts_level == 0;
            // A recreated root may already have the destination UID. Repair its
            // special GID too, including on a retry after a partial migration.
            if (entry->fts_statp->st_uid != previous_uid && !(is_root && entry->fts_statp->st_uid == uid)) continue;
            const gid_t gid = is_root ? policy.root_gid : (entry->fts_number ? policy.cache_gid : uid);
            if (entry->fts_statp->st_uid == uid && entry->fts_statp->st_gid == gid) continue;
            if (lchown(entry->fts_path, uid, gid) != 0) {
                error = errno;
                break;
            }
        }
        if (fts_close(tree) != 0 && error == 0) error = errno;
        errno = error;
        return error == 0 ? 0 : -1;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_xayah_libnative_NativeLib_chownAppDir(JNIEnv *env, jobject, jstring path, jint uid, jint previous_uid) {
    if (!path) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "Path must not be null");
        return -1;
    }
    if (uid < 0 || previous_uid < 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "UIDs must be non-negative full Linux UIDs");
        return -1;
    }
    // JNI modified UTF-8 encodes NUL differently; reject it before filesystem use.
    const jsize length = env->GetStringLength(path);
    const jchar *chars = env->GetStringChars(path, nullptr);
    if (!chars) return -1;
    bool valid = length > 0;
    for (jsize i = 0; i < length; ++i) valid = valid && chars[i] != 0;
    env->ReleaseStringChars(path, chars);
    if (!valid) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Path must be non-empty and contain no NUL");
        return -1;
    }
    const char *native_path = env->GetStringUTFChars(path, nullptr);
    if (!native_path) return -1;
    const int result = chown_app_dir(native_path, static_cast<uid_t>(uid), static_cast<uid_t>(previous_uid));
    if (result != 0) {
        const int error = errno;
        __android_log_print(ANDROID_LOG_ERROR, "NativeLib", "Failed to restore owner of '%s': %s", native_path, strerror(error));
    }
    env->ReleaseStringUTFChars(path, native_path);
    return result;
}
