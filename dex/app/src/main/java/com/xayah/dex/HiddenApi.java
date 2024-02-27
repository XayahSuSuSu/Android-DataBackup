package com.xayah.dex;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.UserHandle;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class HiddenApi {
    public static Context getContext() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (Looper.getMainLooper() == null)
            Looper.prepareMainLooper();
        PrintStream stderr = System.err;
        try {
            System.setErr(new PrintStream("/dev/null"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Object thread = Class.forName("android.app.ActivityThread").getMethod("systemMain").invoke(null);
        Context context = (Context) Class.forName("android.app.ActivityThread").getMethod("getSystemContext").invoke(thread);
        System.setErr(stderr);
        return context;
    }

    public static PackageInfo getPackageInfoAsUser(PackageManager pm, String packageName, int flags, int userId) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (PackageInfo) Class.forName("android.content.pm.PackageManager").getMethod("getPackageInfoAsUser", String.class, int.class, int.class).invoke(pm, packageName, flags, userId);
    }

    public static int getPackageUid(PackageManager pm, String packageName, int flags, int userId) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return getPackageInfoAsUser(pm, packageName, flags, userId).applicationInfo.uid;
    }

    public static void grantRuntimePermission(PackageManager pm, String packageName, String permName, UserHandle user) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class.forName("android.content.pm.PackageManager").getMethod("grantRuntimePermission", String.class, String.class, UserHandle.class).invoke(pm, packageName, permName, user);
    }

    public static void revokeRuntimePermission(PackageManager pm, String packageName, String permName, UserHandle user) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class.forName("android.content.pm.PackageManager").getMethod("revokeRuntimePermission", String.class, String.class, UserHandle.class).invoke(pm, packageName, permName, user);
    }

    public static UserHandle getUserHandle(int userId) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (UserHandle) Class.forName("android.os.UserHandle").getMethod("of", int.class).invoke(null, userId);
    }
}
