package android.os;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/os/UserHandle.java">UserHandle.java</a>
 */
@RefineAs(UserHandle.class)
public class UserHandleHidden {
    /**
     * Range of uids allocated for a user.
     */
    public static final int PER_USER_RANGE = 100000;

    /**
     * A user id constant to indicate the "system" user of the device
     */
    public static final int USER_SYSTEM = 0;

    /**
     * Enable multi-user related side effects. Set this to false if
     * there are problems with single user use-cases.
     */
    public static final boolean MU_ENABLED = true;

    public static UserHandle of(int userId) {
        throw new RuntimeException("Stub!");
    }

    public int getIdentifier() {
        throw new RuntimeException("Stub!");
    }

    public static int getCallingUserId() {
        throw new RuntimeException("Stub!");
    }

    public static int getUserId(int uid) {
        if (MU_ENABLED) {
            return uid / PER_USER_RANGE;
        } else {
            return USER_SYSTEM;
        }
    }
}
