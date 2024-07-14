package android.os;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/os/UserHandle.java">UserHandle.java</a>
 */
@RefineAs(UserHandle.class)
public class UserHandleHidden {
    public static UserHandle of(int userId) {
        throw new RuntimeException("Stub!");
    }

    public int getIdentifier() {
        throw new RuntimeException("Stub!");
    }
}
