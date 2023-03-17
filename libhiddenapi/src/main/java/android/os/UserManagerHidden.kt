package android.os

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.UserInfo
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("NewApi", "PrivateApi")
class UserManagerHidden {
    companion object {
        fun get(context: Context): UserManager {
            return HiddenApiBypass.invoke(Class.forName("android.os.UserManager"), null, "get", context) as UserManager
        }

        @Suppress("UNCHECKED_CAST")
        fun getUsers(userManager: UserManager, excludePartial: Boolean, excludeDying: Boolean, excludePreCreated: Boolean): List<UserInfo> {
            return HiddenApiBypass.invoke(Class.forName("android.os.UserManager"), userManager, "getUsers", excludePartial, excludeDying, excludePreCreated) as List<UserInfo>
        }
    }
}