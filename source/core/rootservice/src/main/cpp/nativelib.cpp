#include <jni.h>
#include <string>
#include <ftw.h>

namespace NativeNS {
    thread_local size_t total_size{0};

    int on_walking(const char *path, const struct stat *p_stat, int flag) {
        total_size += p_stat->st_size;
        return 0;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_xayah_core_rootservice_util_NativeLib_calculateSize(JNIEnv *env, jobject, jstring path) {
    NativeNS::total_size = 0;
    const char *p_path = env->GetStringUTFChars(path, JNI_FALSE);
    ftw(p_path, &NativeNS::on_walking, 1024);
    return NativeNS::total_size;
}