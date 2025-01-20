package com.xayah.dex;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IContentProvider;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HiddenApiHelper {
    public static Context getContext() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
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

        // Setup fake context.
        Application app = new Application();
        Field baseField = ContextWrapper.class.getDeclaredField("mBase");
        baseField.setAccessible(true);
        baseField.set(app, new FakeContext(context));
        Field mInitialApplicationField = Class.forName("android.app.ActivityThread").getDeclaredField("mInitialApplication");
        mInitialApplicationField.setAccessible(true);
        mInitialApplicationField.set(thread, app);

        return context;
    }

    /**
     * <a href="https://github.com/Genymobile/scrcpy/pull/5476">scrcpy #5476</a>
     */
    public static IContentProvider getContentProviderExternal(String name, IBinder token) {
        try {
            Method method;
            Object[] args;
            Class<?> cls = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = cls.getDeclaredMethod("getDefault");
            IInterface am = (IInterface) getDefaultMethod.invoke(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                method = am.getClass().getMethod("getContentProviderExternal", String.class, int.class, IBinder.class, String.class);
                args = new Object[]{name, 0, token, null};
            } else {
                method = am.getClass().getMethod("getContentProviderExternal", String.class, int.class, IBinder.class);
                args = new Object[]{name, 0, token};
            }
            Object providerHolder = method.invoke(am, args);
            if (providerHolder == null) {
                return null;
            }
            Field providerField = providerHolder.getClass().getDeclaredField("provider");
            providerField.setAccessible(true);
            return (IContentProvider) providerField.get(providerHolder);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace(System.out);
            return null;
        }
    }
}
