#include <jni.h>
#include <string>
#include <ftw.h>
#include <sys/stat.h>
#include <climits>

namespace NativeNS {
    thread_local size_t total_size{0};

    int on_walking(const char *path, const struct stat *p_stat, int flag) {
        total_size += p_stat->st_size;
        return 0;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_xayah_libnative_NativeLib_calculateSize(JNIEnv *env, jobject, jstring path) {
    NativeNS::total_size = 0;
    const char *p_path = env->GetStringUTFChars(path, JNI_FALSE);
    ftw(p_path, &NativeNS::on_walking, 1024);
    return NativeNS::total_size;
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
