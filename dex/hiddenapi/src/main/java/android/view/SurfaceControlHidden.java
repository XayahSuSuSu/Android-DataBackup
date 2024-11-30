package android.view;

import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/view/SurfaceControl.java">SurfaceControl.java</a>
 */
@RefineAs(SurfaceControl.class)
public class SurfaceControlHidden {
    public static final int POWER_MODE_OFF = 0;
    public static final int POWER_MODE_NORMAL = 2;

    public static IBinder getBuiltInDisplay(int builtInDisplayId) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @RequiresApi(api = Build.VERSION_CODES.Q)
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/core/java/android/view/SurfaceControl.java;l=1821">SurfaceControl.java</a>
     */
    public static long[] getPhysicalDisplayIds() {
        throw new RuntimeException("Stub!");
    }

    /**
     * @RequiresApi(api = Build.VERSION_CODES.Q)
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/core/java/android/view/SurfaceControl.java;l=1828">SurfaceControl.java</a>
     */
    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        throw new RuntimeException("Stub!");
    }

    public static void setDisplayPowerMode(IBinder displayToken, int mode) {
        throw new RuntimeException("Stub!");
    }
}
