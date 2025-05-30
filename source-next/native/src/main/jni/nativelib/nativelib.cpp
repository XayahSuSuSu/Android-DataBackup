#include <jni.h>
#include <string>
#include <ftw.h>
#include <sys/stat.h>
#include <climits>
#include <fts.h>
#include <android/log.h>

#define LOG_TAG "NativeLib"
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace NativeNS {
    /**
     * https://cs.android.com/android/platform/superproject/+/android-15.0.0_r23:frameworks/native/cmds/installd/utils.cpp;l=467
     */
    int calculate_tree_size(const std::string &path, int64_t *size) {
        FTS *fts;
        FTSENT *p;
        int64_t matchedSize = 0;
        char *argv[] = {(char *) path.c_str(), nullptr};
        if (!(fts = fts_open(argv, FTS_PHYSICAL | FTS_NOCHDIR | FTS_XDEV, nullptr))) {
            if (errno != ENOENT) {
                ALOGE("Failed to fts_open '%s'", path.c_str());
            }
            return -1;
        }
        while ((p = fts_read(fts)) != nullptr) {
            switch (p->fts_info) {
                case FTS_D:
                case FTS_DEFAULT:
                case FTS_F:
                case FTS_SL:
                case FTS_SLNONE:
                    matchedSize += (p->fts_statp->st_blocks * 512);
                    break;
            }
        }
        fts_close(fts);
        *size += matchedSize;
        return 0;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_xayah_libnative_NativeLib_calculateTreeSize(JNIEnv *env, jobject, jstring path) {
    int64_t total_size = 0;
    int64_t *p_total_size = &total_size;
    const char *p_path = env->GetStringUTFChars(path, JNI_FALSE);
    NativeNS::calculate_tree_size(p_path, p_total_size);
    return total_size;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_xayah_libnative_NativeLib_getUidGid(JNIEnv *env, jobject, jstring path) {
    struct stat file_stat{};
    jintArray result = env->NewIntArray(2); // result[0] - uid, result[1] - gid
    jint *p_result = env->GetIntArrayElements(result, nullptr);
    const char *p_path = env->GetStringUTFChars(path, JNI_FALSE);
    if (stat(p_path, &file_stat) == -1) {
        p_result[0] = UINT_MAX;
        p_result[1] = UINT_MAX;
    } else {
        p_result[0] = (int) file_stat.st_uid;
        p_result[1] = (int) file_stat.st_gid;
    }
    env->ReleaseIntArrayElements(result, p_result, 0);
    return result;
}
