package com.xayah.dex;

import android.annotation.SuppressLint;
import android.app.AppOpsManagerHidden;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.UserHandle;

import androidx.core.content.pm.PermissionInfoCompat;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.stream.Collectors;

public class HiddenApiUtil {

    private static void onHelp() {
        System.out.println("HiddenApiUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  getPackageUid USER_ID PACKAGE");
        System.out.println();
        System.out.println("  getRuntimePermissions USER_ID PACKAGE");
        System.out.println();
        System.out.println("  grantRuntimePermission USER_ID PACKAGE PERM_NAME PERM_NAME PERM_NAME ...");
        System.out.println();
        System.out.println("  revokeRuntimePermission USER_ID PACKAGE PERM_NAME PERM_NAME PERM_NAME ...");
        System.out.println();
        System.out.println("  setOpsMode USER_ID PACKAGE OP MODE OP MODE OP MODE ...");
    }

    private static void onCommand(String cmd, String[] args) {
        switch (cmd) {
            case "getPackageUid":
                getPackageUid(args);
            case "getRuntimePermissions":
                getRuntimePermissions(args);
            case "grantRuntimePermission":
                grantRuntimePermission(args);
            case "revokeRuntimePermission":
                revokeRuntimePermission(args);
            case "setOpsMode":
                setOpsMode(args);
            case "help":
                onHelp();
            default:
                System.out.println("Unknown command: " + cmd);
                System.exit(1);
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

    private static void getPackageUid(String[] args) {
        try {
            Context ctx = HiddenApi.getContext();
            int userId = Integer.parseInt(args[1]);
            String packageName = args[2];
            System.out.println(HiddenApi.getPackageUid(ctx.getPackageManager(), packageName, 0, userId));
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    @SuppressLint("ServiceCast")
    private static void getRuntimePermissions(String[] args) {
        try {
            Context ctx = HiddenApi.getContext();
            PackageManager packageManager = ctx.getPackageManager();
            AppOpsManagerHidden appOpsManager = (AppOpsManagerHidden) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int userId = Integer.parseInt(args[1]);
            String packageName = args[2];
            PackageInfo packageInfo = HiddenApi.getPackageInfoAsUser(packageManager, packageName, PackageManager.GET_PERMISSIONS, userId);
            String[] requestedPermissions = packageInfo.requestedPermissions;
            int[] requestedPermissionsFlags = packageInfo.requestedPermissionsFlags;
            AppOpsManagerHidden.PackageOps ops = null;
            try {
                ops = appOpsManager.getOpsForPackage(packageInfo.applicationInfo.uid, packageName, null).get(0);
            } catch (Exception ignored) {
            }
            Map<Integer, Integer> opsMap = null;
            if (ops != null) {
                opsMap = ops.getOps().stream().collect(Collectors.toMap(AppOpsManagerHidden.OpEntry::getOp, AppOpsManagerHidden.OpEntry::getMode));
            }
            for (int i = 0; i < requestedPermissions.length; i++) {
                try {
                    PermissionInfo permissionInfo = packageManager.getPermissionInfo(requestedPermissions[i], 0);
                    int protection = PermissionInfoCompat.getProtection(permissionInfo);
                    int protectionFlags = PermissionInfoCompat.getProtectionFlags(permissionInfo);
                    boolean isGranted = (requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                    int op = AppOpsManagerHidden.permissionToOpCode(requestedPermissions[i]);
                    int mode = AppOpsManagerHidden.MODE_IGNORED;
                    if (opsMap != null) {
                        mode = opsMap.getOrDefault(op, AppOpsManagerHidden.MODE_IGNORED);
                    }
                    if ((op != AppOpsManagerHidden.OP_NONE)
                            || (protection == PermissionInfo.PROTECTION_DANGEROUS || (protectionFlags & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0)) {
                        System.out.println(requestedPermissions[i] + " " + isGranted + " " + op + " " + mode);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void grantRuntimePermission(String[] args) {
        try {
            Context ctx = HiddenApi.getContext();
            int userId = Integer.parseInt(args[1]);
            String packageName = args[2];
            UserHandle user = HiddenApi.getUserHandle(userId);
            for (int i = 3; i < args.length; i++) {
                String[] permNames = args[i].split(" ");
                for (String permName : permNames) {
                    try {
                        HiddenApi.grantRuntimePermission(ctx.getPackageManager(), packageName, permName, user);
                    } catch (InvocationTargetException e) {
                        System.out.println("Failed, skip: " + permName);
                    }
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void revokeRuntimePermission(String[] args) {
        try {
            Context ctx = HiddenApi.getContext();
            int userId = Integer.parseInt(args[1]);
            String packageName = args[2];
            UserHandle user = HiddenApi.getUserHandle(userId);
            for (int i = 3; i < args.length; i++) {
                String[] permNames = args[i].split(" ");
                for (String permName : permNames) {
                    try {
                        HiddenApi.revokeRuntimePermission(ctx.getPackageManager(), packageName, permName, user);
                    } catch (InvocationTargetException e) {
                        System.out.println("Failed, skip: " + permName);
                    }
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    @SuppressLint("ServiceCast")
    private static void setOpsMode(String[] args) {
        try {
            Context ctx = HiddenApi.getContext();
            PackageManager packageManager = ctx.getPackageManager();
            AppOpsManagerHidden appOpsManager = (AppOpsManagerHidden) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int userId = Integer.parseInt(args[1]);
            String packageName = args[2];
            PackageInfo packageInfo = HiddenApi.getPackageInfoAsUser(packageManager, packageName, PackageManager.GET_PERMISSIONS, userId);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                stringBuilder.append(args[i].trim());
                stringBuilder.append(" ");
            }
            String[] opSet = stringBuilder.toString().trim().split(" ");
            for (int i = 0; i < opSet.length; i += 2) {
                try {
                    int op = Integer.parseInt(opSet[i]);
                    int mode = Integer.parseInt(opSet[i + 1]);
                    appOpsManager.setMode(op, packageInfo.applicationInfo.uid, packageName, mode);
                } catch (Exception e) {
                    System.out.println("Failed, skip: " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
