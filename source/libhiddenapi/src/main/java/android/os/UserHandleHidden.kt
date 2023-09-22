package android.os

import android.annotation.SuppressLint
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
object UserHandleHidden {
    fun of(userId: Int): UserHandle {
        return HiddenApiBypass.invoke(Class.forName("android.os.UserHandle"), null, "of", userId) as UserHandle
    }
}
