package com.xayah.dex;

import android.annotation.SuppressLint;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandleHidden;

import androidx.core.app.NotificationCompat;

import java.util.Collections;

public class NotificationUtil extends BaseUtil {
    public static final int SHELL_UID = 2000;
    public static final String SHELL_PACKAGE = "com.android.shell";
    public static final int NOTIFICATION_ID = 2020;

    private static final String SYSTEM_NOTIFICATION_CHANNEL_ALERTS = "ALERTS";

    public static final String SHELL_CHANNEL_ID = "shell_cmd";
    public static final String SHELL_CHANNEL_NAME = "Shell command";
    public static final int SHELL_CHANNEL_IMP = NotificationManager.IMPORTANCE_DEFAULT;

    private static INotificationManager sService;

    private static void onHelp() {
        System.out.println("NotificationUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  notify [flags] <tag> <text>");
        System.out.println();
        System.out.println();
        System.out.println("flags:");
        System.out.println("  -t|--title <text>");
        System.out.println("  -p|--progress <max> <progress> <indeterminate>");
        System.out.println();
        System.out.println("examples:");
        System.out.println("  notify -t 'This is title' 'This is tag' 'This is content.'");
        System.out.println("  notify -p 100 50 false -t 'This is title' 'This is tag' 'This is content.'");
        System.out.println("  notify -p 0 0 true -t 'This is title' 'This is tag' 'This is content.'");
    }

    private static void onCommand() {
        final int callingUid = Binder.getCallingUid();
        switch (mCmd) {
            case "help":
                onHelp();
                break;
            case "notify":
                notify(callingUid);
                break;
            default:
                System.out.println("Unknown command: " + mCmd);
                System.exit(1);
                break;
        }
    }

    public static void main(String[] args) {
        getService();
        if (sService == null) {
            System.out.println("Failed to get service.");
            System.exit(1);
        }

        mArgs = args;
        if (args != null && args.length > 0) {
            mCmd = args[0];
            mArgPos = 1;
            onCommand();
        } else {
            onHelp();
        }
        System.exit(0);
    }

    @SuppressLint("NotificationPermission")
    private static void notify(int callingUid) {
        try {
            final Context ctx = HiddenApiHelper.getContext();
            NotificationCompat.Builder builder;

            if (callingUid == SHELL_UID) {
                ensureChannel(SHELL_PACKAGE);
                builder = new NotificationCompat.Builder(ctx, SHELL_CHANNEL_ID);
            } else {
                builder = new NotificationCompat.Builder(ctx, SYSTEM_NOTIFICATION_CHANNEL_ALERTS);
            }

            String opt;

            while ((opt = getNextOption()) != null) {
                switch (opt) {
                    case "-t":
                    case "--title":
                        builder.setContentTitle(getNextArgRequired());
                        break;
                    case "-p":
                    case "--progress":
                        final int max = Integer.parseInt(getNextArgRequired());
                        final int progress = Integer.parseInt(getNextArgRequired());
                        final boolean indeterminate = getNextArgRequired().equals("true");
                        builder.setProgress(max, progress, indeterminate);
                        break;
                }
            }

            final String tag = getNextArg();
            final String text = getNextArg();
            if (tag == null || text == null) {
                onHelp();
                System.exit(1);
            }

            builder.setContentText(text);
            builder.setSmallIcon(android.R.drawable.sym_def_app_icon);

            final Notification n = builder.build();

            if (callingUid == SHELL_UID) {
                sService.enqueueNotificationWithTag(SHELL_PACKAGE, SHELL_PACKAGE, tag,
                        NOTIFICATION_ID, n, UserHandleHidden.getUserId(callingUid));
            } else {
                final NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(tag, NOTIFICATION_ID, n);
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static INotificationManager getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService("notification");
        sService = INotificationManager.Stub.asInterface(b);
        return sService;
    }

    static void ensureChannel(String callingPackage) throws RemoteException {
        final NotificationChannel channel =
                new NotificationChannel(SHELL_CHANNEL_ID, SHELL_CHANNEL_NAME, SHELL_CHANNEL_IMP);
        sService.createNotificationChannels(callingPackage,
                new ParceledListSlice<>(Collections.singletonList(channel)));
    }
}
