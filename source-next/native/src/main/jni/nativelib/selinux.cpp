#include <jni.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <xdl.h>

namespace NativeNS {
    constexpr char kSelinuxLibraryName[] = "libselinux.so";
    constexpr char kRestoreconSymbol[] = "selinux_android_restorecon";
    constexpr char kRestoreconPkgdirSymbol[] = "selinux_android_restorecon_pkgdir";

    struct SelinuxFunctions {
        using Restorecon = int (*)(const char *, unsigned int);
        using RestoreconPkgdir = int (*)(const char *, const char *, uid_t, unsigned int);

        Restorecon restorecon = nullptr;
        RestoreconPkgdir restorecon_pkgdir = nullptr;

        SelinuxFunctions() {
            // Keep the library loaded for the lifetime of its cached function pointers.
            void *p_library = dlopen(kSelinuxLibraryName, RTLD_NOW | RTLD_LOCAL);
            if (p_library) {
                restorecon = reinterpret_cast<Restorecon>(dlsym(p_library, kRestoreconSymbol));
                restorecon_pkgdir = reinterpret_cast<RestoreconPkgdir>(dlsym(p_library, kRestoreconPkgdirSymbol));
            }
            if (restorecon && restorecon_pkgdir) {
                return;
            }

            // Look up the system ELF already held by AndroidRuntime across linker namespaces.
            void *p_handle = xdl_open(kSelinuxLibraryName, XDL_DEFAULT);
            if (!p_handle) {
                return;
            }
            if (!restorecon) {
                restorecon = reinterpret_cast<Restorecon>(xdl_sym(p_handle, kRestoreconSymbol, nullptr));
            }
            if (!restorecon_pkgdir) {
                restorecon_pkgdir = reinterpret_cast<RestoreconPkgdir>(xdl_sym(p_handle, kRestoreconPkgdirSymbol, nullptr));
            }
            xdl_close(p_handle); // Frees lookup metadata
        }
    };

    static const SelinuxFunctions &selinux_functions() {
        static const SelinuxFunctions functions;
        return functions;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_xayah_libnative_NativeLib_selinuxAndroidRestorecon(JNIEnv *env, jobject, jstring path, jint flags) {
    if (!path) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "Path must not be null");
        return -1;
    }
    const auto restorecon = NativeNS::selinux_functions().restorecon;
    if (!restorecon) {
        env->ThrowNew(env->FindClass("java/lang/UnsupportedOperationException"), "Cannot resolve selinux_android_restorecon via dlopen or xDL");
        return -1;
    }
    const char *p_path = env->GetStringUTFChars(path, nullptr);
    if (!p_path) {
        return -1;
    }
    const int result = restorecon(p_path, static_cast<unsigned int>(flags));
    env->ReleaseStringUTFChars(path, p_path);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_xayah_libnative_NativeLib_selinuxAndroidRestoreconPkgdir(
        JNIEnv *env, jobject, jstring path, jstring seinfo, jint uid, jint flags) {
    if (!path || !seinfo) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "Path and seinfo must not be null");
        return -1;
    }
    const auto restorecon = NativeNS::selinux_functions().restorecon_pkgdir;
    if (!restorecon) {
        env->ThrowNew(env->FindClass("java/lang/UnsupportedOperationException"),
                      "Cannot resolve selinux_android_restorecon_pkgdir via dlopen or xDL");
        return -1;
    }
    const char *p_path = env->GetStringUTFChars(path, nullptr);
    if (!p_path) {
        return -1;
    }
    const char *p_seinfo = env->GetStringUTFChars(seinfo, nullptr);
    if (!p_seinfo) {
        env->ReleaseStringUTFChars(path, p_path);
        return -1;
    }
    const int result = restorecon(p_path, p_seinfo, static_cast<uid_t>(uid), static_cast<unsigned int>(flags));
    env->ReleaseStringUTFChars(seinfo, p_seinfo);
    env->ReleaseStringUTFChars(path, p_path);
    return result;
}
