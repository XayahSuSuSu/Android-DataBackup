package android.os;

import android.content.Context;
import android.content.pm.UserInfo;

import java.util.List;

import dev.rikka.tools.refine.RefineAs;

/**
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/os/UserManager.java">UserManager.java</a>
 */
@RefineAs(UserManager.class)
public class UserManagerHidden {
    public static UserManager get(Context context) {
        throw new RuntimeException("Stub!");
    }

    public List<UserInfo> getUsers() {
        throw new RuntimeException("Stub!");
    }
}

