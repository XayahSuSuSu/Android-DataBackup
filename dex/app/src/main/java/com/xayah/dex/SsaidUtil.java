package com.xayah.dex;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerHidden;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Process;

import com.android.providers.settings.SettingsState;
import com.android.providers.settings.SettingsStateApi26;
import com.android.providers.settings.SettingsStateApi31;

import java.io.File;
import java.util.Objects;

import dev.rikka.tools.refine.Refine;

public class SsaidUtil {
    private static final String SSAID_USER_KEY = "userkey";

    private static SettingsState getSettingsState(int userId) {
        Object lock = new Object();
        HandlerThread thread = new HandlerThread("ssaid_handler", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        File file = new File("/data/system/users/" + userId + "/settings_ssaid.xml");
        int key = SettingsState.makeKey(SettingsState.SETTINGS_TYPE_SSAID, userId);
        SettingsState settingsState;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            settingsState = new SettingsStateApi31(
                    lock,
                    file,
                    key,
                    SettingsState.MAX_BYTES_PER_APP_PACKAGE_UNLIMITED,
                    thread.getLooper()
            );
        } else {
            settingsState = new SettingsStateApi26(
                    lock,
                    file,
                    key,
                    SettingsState.MAX_BYTES_PER_APP_PACKAGE_UNLIMITED,
                    thread.getLooper()
            );
        }
        return settingsState;
    }

    private static void onHelp() {
        System.out.println("Ssaid commands:");
        System.out.println("  help");
        System.out.println("    Print this help text.");
        System.out.println();
        System.out.println("  get USER_ID PACKAGE PACKAGE PACKAGE ...");
        System.out.println("    Get ssaid.");
        System.out.println();
        System.out.println("  set USER_ID PACKAGE SSAID PACKAGE SSAID PACKAGE SSAID ...");
        System.out.println("    Set ssaid.");
    }

    private static void onGet(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManager pm = ctx.getPackageManager();
            PackageManagerHidden pmHidden = Refine.unsafeCast(pm);
            int userId = Integer.parseInt(args[1]);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                stringBuilder.append(args[i].trim());
                stringBuilder.append(" ");
            }
            String[] pkgSet = stringBuilder.toString().trim().split(" ");
            for (String packageName : pkgSet) {
                PackageInfo packageInfo = pmHidden.getPackageInfoAsUser(packageName, 0, userId);
                int uid = packageInfo.applicationInfo.uid;
                SettingsState settingsState = getSettingsState(userId);
                System.out.println(packageName + " " + settingsState.getSettingLocked(getName(packageName, uid)).getValue());
            }
            System.exit(0);
        } catch (Exception e) {
            System.out.printf("Failed: %s, %s\n", e.getCause(), e.getMessage());
            onHelp();
            System.exit(1);
        }
    }

    private static void onSet(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManager pm = ctx.getPackageManager();
            PackageManagerHidden pmHidden = Refine.unsafeCast(pm);
            int userId = Integer.parseInt(args[1]);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                stringBuilder.append(args[i].trim());
                stringBuilder.append(" ");
            }
            String[] ssaidSet = stringBuilder.toString().trim().split(" ");
            for (int i = 0; i < ssaidSet.length; i += 2) {
                try {
                    String packageName = ssaidSet[i];
                    String ssaid = ssaidSet[i + 1];
                    PackageInfo packageInfo = pmHidden.getPackageInfoAsUser(packageName, 0, userId);
                    int uid = packageInfo.applicationInfo.uid;
                    SettingsState settingsState = getSettingsState(userId);
                    settingsState.insertSettingLocked(getName(packageName, uid), ssaid, null, true, packageName);
                } catch (Exception e) {
                    System.out.println("Failed, skip: " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            System.out.printf("Failed: %s, %s\n", e.getCause(), e.getMessage());
            onHelp();
            System.exit(1);
        }
    }

    private static void onCommand(String cmd, String[] args) {
        switch (cmd) {
            case "get":
                onGet(args);
                break;
            case "set":
                onSet(args);
                break;
            default:
                onHelp();
                break;
        }
    }

    public static void main(String[] args) {
        String cmd;
        if (args != null && args.length > 0) {
            cmd = args[0];
            onCommand(cmd, args);
        } else {
            onHelp();
        }
        System.exit(0);
    }

    private static String getName(String packageName, int uid) {
        if (Objects.equals(packageName, SettingsState.SYSTEM_PACKAGE_NAME))
            return SSAID_USER_KEY;
        else
            return String.valueOf(uid);
    }
}
