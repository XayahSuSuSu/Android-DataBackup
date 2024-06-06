package android.os;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/os/ServiceManager.java">ServiceManager.java</a>
 */
public class ServiceManagerHidden {
    public static IBinder getService(String name) {
        throw new RuntimeException("Stub!");
    }
}
