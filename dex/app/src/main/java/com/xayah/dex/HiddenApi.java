package com.xayah.dex;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class HiddenApi {
    public static Context getContext() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Looper.prepare();
        Object thread = Class.forName("android.app.ActivityThread").getMethod("systemMain").invoke(null);
        Context context = (Context) Class.forName("android.app.ActivityThread").getMethod("getSystemContext").invoke(thread);
        Objects.requireNonNull(Looper.myLooper()).quit();
        return context;
    }

    public static PackageInfo getPackageInfoAsUser(PackageManager pm, String packageName, int flags, int userId) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (PackageInfo) Class.forName("android.content.pm.PackageManager").getMethod("getPackageInfoAsUser", String.class, int.class, int.class).invoke(pm, packageName, flags, userId);
    }

    public static int getPackageUid(PackageManager pm, String packageName, int flags, int userId) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return getPackageInfoAsUser(pm, packageName, flags, userId).applicationInfo.uid;
    }
}
