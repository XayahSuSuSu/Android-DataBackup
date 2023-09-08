package android.os

import android.annotation.SuppressLint
import org.lsposed.hiddenapibypass.HiddenApiBypass

@SuppressLint("PrivateApi")
object ServiceManagerHidden {
    fun getService(name: String): IBinder {
        return HiddenApiBypass.invoke(Class.forName("android.os.ServiceManager"), null, "getService", name) as IBinder
    }
}
