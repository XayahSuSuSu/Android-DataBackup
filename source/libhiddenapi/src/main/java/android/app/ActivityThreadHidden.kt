package android.app

import android.annotation.SuppressLint
import android.content.Context
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
object ActivityThreadHidden {
    fun systemMain(): Any {
        return HiddenApiBypass.invoke(Class.forName("android.app.ActivityThread"), null, "systemMain")
    }

    fun getSystemContext(activityThread: Any): Context {
        return HiddenApiBypass.invoke(Class.forName("android.app.ActivityThread"), activityThread, "getSystemContext") as Context
    }
}
