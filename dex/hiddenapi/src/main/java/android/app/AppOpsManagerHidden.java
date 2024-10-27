package android.app;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:frameworks/base/core/java/android/app/AppOpsManager.java">AppOpsManager.java</a>
 */
@RefineAs(AppOpsManager.class)
public class AppOpsManagerHidden {
    public static final int MODE_IGNORED = 1;
    public static final int OP_NONE = -1;
    public static final int _NUM_OP = 64;

    public List<PackageOps> getOpsForPackage(int uid, String packageName, int[] ops) {
        throw new RuntimeException("Stub!");
    }

    public static int permissionToOpCode(String permission) {
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
