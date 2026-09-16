#include <jni.h>
#include <android/log.h>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <string>
#include <sys/stat.h>
#include <sys/xattr.h>

// https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/native/cmds/installd/utils.cpp
namespace {
    int get_path_inode(const std::string &path, ino_t *inode) {
        struct stat buf{};
        if (stat(path.c_str(), &buf) != 0) {
            __android_log_print(ANDROID_LOG_WARN, "NativeLib", "Failed to stat %s: %s", path.c_str(), strerror(errno));
            return -1;
        }
        *inode = buf.st_ino;
        return 0;
    }

    /**
     * Write the inode of a specific child file into the given xattr on the
     * parent directory. This allows you to find the child later, even if its
     * name is encrypted.
     */
    int write_path_inode(const std::string &parent, const char *name, const char *inode_xattr) {
        ino_t inode = 0;
        uint64_t inode_raw = 0;
        const auto path = parent + "/" + name;
        if (get_path_inode(path, &inode) != 0) {
            // Path probably doesn't exist yet; ignore
            return 0;
        }

        // Check to see if already set correctly
        if (getxattr(parent.c_str(), inode_xattr, &inode_raw, sizeof(inode_raw)) == sizeof(inode_raw)) {
            if (inode_raw == inode) {
                // Already set correctly; skip writing
                return 0;
            }
            __android_log_print(ANDROID_LOG_WARN, "NativeLib", "Mismatched inode at %s; overwriting %s", path.c_str(), inode_xattr);
        }

        inode_raw = inode;
        if (setxattr(parent.c_str(), inode_xattr, &inode_raw, sizeof(inode_raw), 0) != 0 && errno != EOPNOTSUPP) {
            __android_log_print(ANDROID_LOG_ERROR, "NativeLib", "Failed to write xattr %s at %s: %s",
                                inode_xattr, parent.c_str(), strerror(errno));
            return -1;
        }
        return 0;
    }

    bool read_string(JNIEnv *env, jstring value, std::string &result) {
        if (!value) {
            env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "Arguments must not be null");
            return false;
        }
        const jsize length = env->GetStringLength(value);
        const jchar *chars = env->GetStringChars(value, nullptr);
        if (!chars) return false;
        bool valid = length > 0;
        for (jsize i = 0; i < length; ++i) valid = valid && chars[i] != 0;
        env->ReleaseStringChars(value, chars);
        if (!valid) {
            env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Arguments must not be empty or contain NUL");
            return false;
        }
        const char *utf = env->GetStringUTFChars(value, nullptr);
        if (!utf) return false;
        result = utf;
        env->ReleaseStringUTFChars(value, utf);
        return true;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_xayah_libnative_NativeLib_writePathInode(JNIEnv *env, jobject, jstring parent, jstring name, jstring inode_xattr) {
    std::string native_parent, native_name, native_inode_xattr;
    if (!read_string(env, parent, native_parent) || !read_string(env, name, native_name) || !read_string(env, inode_xattr, native_inode_xattr)) {
        return -1;
    }
    return write_path_inode(native_parent, native_name.c_str(), native_inode_xattr.c_str());
}
