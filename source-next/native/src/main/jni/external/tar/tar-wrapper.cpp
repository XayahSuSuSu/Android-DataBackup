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
#include <asm-generic/fcntl.h>
#include <fcntl.h>
#include <sys/prctl.h>

#define LOG_TAG "Tar-Wrapper"
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern int main(int argc, char **argv);

extern "C" JNIEXPORT jint JNICALL
Java_com_xayah_libnative_TarWrapper_callCli(JNIEnv *env, jobject, jstring std_out, jstring std_err, jobjectArray j_argv) {
    const char *out_path = env->GetStringUTFChars(std_out, nullptr);
    const char *err_path = env->GetStringUTFChars(std_err, nullptr);

    // Save previous STDOUT/STDERR
    int saved_stdout = dup(STDOUT_FILENO);
    int saved_stderr = dup(STDERR_FILENO);

    int out_fd = open(out_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    int err_fd = open(err_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);

    // Release jni strings
    env->ReleaseStringUTFChars(std_out, out_path);
    env->ReleaseStringUTFChars(std_err, err_path);

    if (out_fd == -1 || err_fd == -1) {
        ALOGE("Failed to open STDOUT/STDERR files.");
        if (out_fd != -1) close(out_fd);
        if (err_fd != -1) close(err_fd);
        if (saved_stdout != -1) close(saved_stdout);
        if (saved_stderr != -1) close(saved_stderr);
        return -1;
    }

    pid_t pid = fork();
    if (pid == 0) {
        // Set the parent death signal to SIGTERM: if the parent process exits,
        // the kernel will send SIGTERM to this process.
        prctl(PR_SET_PDEATHSIG, SIGTERM);
        if (getppid() == 1) {
            _exit(SIGTERM);
        }

        // Redirect STDOUT/STDERR
        dup2(out_fd, STDOUT_FILENO);
        dup2(err_fd, STDERR_FILENO);

        // Close idle fds
        close(out_fd);
        close(err_fd);
        close(saved_stdout);
        close(saved_stderr);

        // Build argv
        jsize argc = env->GetArrayLength(j_argv);
        char **argv = (char **) malloc((argc + 1) * sizeof(char *));
        for (int i = 0; i < argc; i++) {
            auto arg_str = (jstring) env->GetObjectArrayElement(j_argv, i);
            const char *arg_utf = env->GetStringUTFChars(arg_str, nullptr);
            argv[i] = strdup(arg_utf);  // Copy then release to avoid memory leak
            env->ReleaseStringUTFChars(arg_str, arg_utf);
            env->DeleteLocalRef(arg_str);
        }
        argv[argc] = nullptr;

        int result = main(argc, argv);

        // Release string resources
        for (int i = 0; i < argc; i++) {
            free(argv[i]);
        }
        free(argv);
        _exit(result);
    } else if (pid > 0) {
        // Close idle fds
        close(out_fd);
        close(err_fd);
        close(saved_stdout);
        close(saved_stderr);

        int exit_status;

        // Wait for child process to exit.
        if (waitpid(pid, &exit_status, 0) == -1) {
            ALOGE("Failed to get exit status.");
            return -1;
        }

        // Get the exit code from child process
        if (WIFEXITED(exit_status)) {
            return WEXITSTATUS(exit_status);
        } else {
            return -1;
        }
    } else {
        ALOGE("Failed to fork.");
        close(out_fd);
        close(err_fd);
        close(saved_stdout);
        close(saved_stderr);
        return -1;
    }
}
