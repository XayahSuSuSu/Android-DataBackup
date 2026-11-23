#include <jni.h>
#include <string>
#include <cstring>
#include <cstdlib>
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

    // O_CLOEXEC keeps tar processes forked concurrently for other archives from inheriting these files.
    // dup2 clears the flag on the child's STDOUT/STDERR copies.
    int out_fd = open(out_path, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    int err_fd = open(err_path, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);

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

    // Build argv before forking. After fork() only the calling thread exists in the child, so JNI and runtime locks
    // held by other threads (GC, other binder calls archiving apps concurrently) would never be released there.
    jsize argc = env->GetArrayLength(j_argv);
    char **argv = (char **) calloc(argc + 1, sizeof(char *));
    if (argv == nullptr) {
        ALOGE("Failed to allocate argv.");
        close(out_fd);
        close(err_fd);
        if (saved_stdout != -1) close(saved_stdout);
        if (saved_stderr != -1) close(saved_stderr);
        return -1;
    }
    for (int i = 0; i < argc; i++) {
        auto arg_str = (jstring) env->GetObjectArrayElement(j_argv, i);
        const char *arg_utf = env->GetStringUTFChars(arg_str, nullptr);
        argv[i] = strdup(arg_utf);  // Copy then release to avoid memory leak
        env->ReleaseStringUTFChars(arg_str, arg_utf);
        env->DeleteLocalRef(arg_str);
    }
    argv[argc] = nullptr;

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

        int result = main(argc, argv);
        _exit(result);
    }

    // Release string resources in the parent; the child exits through _exit().
    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);

    if (pid > 0) {
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
