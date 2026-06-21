package android.app;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/core/java/android/app/AppOpsManager.java">AppOpsManager.java</a>
 */
@RefineAs(AppOpsManager.class)
public class AppOpsManagerHidden {
    public static final int MODE_ALLOWED = 0;
    public static final int MODE_IGNORED = 1;
    public static final int MODE_ERRORED = 2;
    public static final int MODE_DEFAULT = 3;

    /**
     * @RequiresApi(api = Build.VERSION_CODES.P)
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/app/AppOpsManager.java;l=119">AppOpsManager.java</a>
     */
    public static final int MODE_FOREGROUND = 4;
    public static final int OP_NONE = -1;

    public List<PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        throw new RuntimeException("Stub!");
    }

    public static int permissionToOpCode(String permission) {
        throw new RuntimeException("Stub!");
    }

    public static int strOpToOp(String op) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @RequiresApi(api = Build.VERSION_CODES.Q)
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/core/java/android/app/AppOpsManager.java;l=2048">AppOpsManager.java</a>
     */
    public static String opToPublicName(int op) {
        throw new RuntimeException("Stub!");
    }

    public static String opToName(int op) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @RequiresApi(api = Build.VERSION_CODES.Q)
     * @see <a href="https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/core/java/android/app/AppOpsManager.java;l=4995">AppOpsManager.java</a>
     */
    public int unsafeCheckOpRawNoThrow(int op, int uid, String packageName) {
        throw new RuntimeException("Stub!");
    }

    public int checkOpNoThrow(int op, int uid, String packageName) {
        throw new RuntimeException("Stub!");
    }

    public void setMode(int code, int uid, String packageName, int mode) {
        throw new RuntimeException("Stub!");
    }

    public static class PackageOps {
        public String getPackageName() {
            throw new RuntimeException("Stub!");
        }

        public int getUid() {
            throw new RuntimeException("Stub!");
        }

        public List<OpEntry> getOps() {
            throw new RuntimeException("Stub!");
        }
    }

    public static class OpEntry {
        public int getOp() {
            throw new RuntimeException("Stub!");
        }

        public int getMode() {
            throw new RuntimeException("Stub!");
        }
    }
}
