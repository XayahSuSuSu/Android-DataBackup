package com.android.server.display;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

/**
 * @author <a href="https://github.com/rom1v">@rom1v</a>
 * @see <a href="https://cs.android.com/android/platform/superproject/+/android-14.0.0_r37:frameworks/base/services/core/java/com/android/server/display/DisplayControl.java">DisplayControl.java</a>
 */
@RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "BlockedPrivateApi"})
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public final class DisplayControlHidden {
    private static final Class<?> CLASS;

    static {
        Class<?> displayControlClass = null;
        try {
            Class<?> classLoaderFactoryClass = Class.forName("com.android.internal.os.ClassLoaderFactory");
            Method createClassLoaderMethod = classLoaderFactoryClass.getDeclaredMethod("createClassLoader", String.class, String.class, String.class,
                    ClassLoader.class, int.class, boolean.class, String.class);
            ClassLoader classLoader = (ClassLoader) createClassLoaderMethod.invoke(null, "/system/framework/services.jar", null, null,
                    ClassLoader.getSystemClassLoader(), 0, true, null);

            displayControlClass = classLoader.loadClass("com.android.server.display.DisplayControl");

            Method loadMethod = Runtime.class.getDeclaredMethod("loadLibrary0", Class.class, String.class);
            loadMethod.setAccessible(true);
            loadMethod.invoke(Runtime.getRuntime(), displayControlClass, "android_servers");
        } catch (Throwable e) {
            System.out.println("Could not initialize DisplayControl: " + e);
            e.printStackTrace(System.out);
            // Do not throw an exception here, the methods will fail when they are called
        }
        CLASS = displayControlClass;
    }

    private static Method getPhysicalDisplayTokenMethod;
    private static Method getPhysicalDisplayIdsMethod;

    private DisplayControlHidden() {
        // only static methods
    }

    private static Method getGetPhysicalDisplayTokenMethod() throws NoSuchMethodException {
        if (getPhysicalDisplayTokenMethod == null) {
            getPhysicalDisplayTokenMethod = CLASS.getMethod("getPhysicalDisplayToken", long.class);
        }
        return getPhysicalDisplayTokenMethod;
    }

    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        try {
            Method method = getGetPhysicalDisplayTokenMethod();
            return (IBinder) method.invoke(null, physicalDisplayId);
        } catch (ReflectiveOperationException e) {
            System.out.println("Could not invoke method: " + e);
            e.printStackTrace(System.out);
            return null;
        }
    }

    private static Method getGetPhysicalDisplayIdsMethod() throws NoSuchMethodException {
        if (getPhysicalDisplayIdsMethod == null) {
            getPhysicalDisplayIdsMethod = CLASS.getMethod("getPhysicalDisplayIds");
        }
        return getPhysicalDisplayIdsMethod;
    }

    public static long[] getPhysicalDisplayIds() {
        try {
            Method method = getGetPhysicalDisplayIdsMethod();
            return (long[]) method.invoke(null);
        } catch (ReflectiveOperationException e) {
            System.out.println("Could not invoke method: " + e);
            e.printStackTrace(System.out);
            return null;
        }
    }
}
