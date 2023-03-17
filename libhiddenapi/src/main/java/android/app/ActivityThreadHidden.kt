package android.app

import android.annotation.SuppressLint
import android.content.Context
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("NewApi", "PrivateApi")
class ActivityThreadHidden {
    companion object {
        fun systemMain(): Any {
            return HiddenApiBypass.invoke(Class.forName("android.app.ActivityThread"), null, "systemMain")
        }

        fun getSystemContext(activityThread: Any): Context {
            return HiddenApiBypass.invoke(Class.forName("android.app.ActivityThread"), activityThread, "getSystemContext") as Context
        }
    }
}