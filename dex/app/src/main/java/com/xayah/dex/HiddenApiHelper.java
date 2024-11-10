package com.xayah.dex;

import android.content.Context;
import android.os.Looper;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

public class HiddenApiHelper {
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
}
