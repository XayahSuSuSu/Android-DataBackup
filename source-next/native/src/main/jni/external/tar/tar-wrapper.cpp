#include <jni.h>
#include <string>
#include <ftw.h>
#include <sys/stat.h>
#include <climits>
#include <fts.h>
#include <android/log.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>
#include <fstream>

#define LOG_TAG "Tar-Wrapper"
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern int main(int argc, char **argv);

extern "C" JNIEXPORT jint JNICALL
Java_com_xayah_libnative_TarWrapper_callCli(JNIEnv *env, jobject, jstring std_out, jstring std_err, jobjectArray j_argv) {
    pid_t pid = fork();
    if (pid == 0) {
        FILE *std_out_file = freopen((char *) env->GetStringUTFChars(std_out, nullptr), "w", stdout);
        if (std_out_file == nullptr) {
            ALOGE("Failed to redirect std out.");
            return -1;
        }
        FILE *std_err_file = freopen((char *) env->GetStringUTFChars(std_err, nullptr), "w", stderr);
        if (std_err_file == nullptr) {
            ALOGE("Failed to redirect std err.");
            return -1;
        }

        jsize argc = env->GetArrayLength(j_argv);
        char **argv = (char **) malloc(argc * sizeof(char *));
        int i = 0;
        for (i = 0; i < argc; i++) {
            auto arg = (jstring) env->GetObjectArrayElement(j_argv, i);
            argv[i] = (char *) env->GetStringUTFChars(arg, nullptr);
        }
        main(argc, argv);
        free(argv);
    } else if (pid > 0) {
        int exit_status;
        pid_t result = waitpid(pid, &exit_status, 0);
        if (result == -1) {
            ALOGE("Failed to get exit status.");
            return -1;
        } else if (WIFEXITED(exit_status)) {
            return WEXITSTATUS(exit_status);
        }
    } else {
        ALOGE("Failed to fork process.");
        return -1;
    }
    return -1;
}
