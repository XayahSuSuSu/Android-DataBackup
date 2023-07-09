package android.os

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.UserInfo
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
@Suppress("UNCHECKED_CAST")
class UserManagerHidden {
    companion object {
        fun get(context: Context): UserManager {
            return HiddenApiBypass.invoke(Class.forName("android.os.UserManager"), null, "get", context) as UserManager
        }

        /**
         * @RequiresApi(Build.VERSION_CODES.R)
         * Ref: https://cs.android.com/android/platform/superproject/+/android-11.0.0_r45:frameworks/base/core/java/android/os/UserManager.java;l=3210
         */
        fun getUsers(userManager: UserManager, excludePartial: Boolean, excludeDying: Boolean, excludePreCreated: Boolean): List<UserInfo> {
            return HiddenApiBypass.invoke(Class.forName("android.os.UserManager"), userManager, "getUsers", excludePartial, excludeDying, excludePreCreated) as List<UserInfo>
        }

        /**
         * Universal solution
         */
        fun getUsers(userManager: UserManager): List<UserInfo> {
            return HiddenApiBypass.invoke(Class.forName("android.os.UserManager"), userManager, "getUsers") as List<UserInfo>
        }
    }
}