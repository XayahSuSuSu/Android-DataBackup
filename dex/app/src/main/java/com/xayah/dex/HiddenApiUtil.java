package com.xayah.dex;

import android.annotation.SuppressLint;
import android.app.AppOpsManagerHidden;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerHidden;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.view.SurfaceControlHidden;

import androidx.core.content.pm.PermissionInfoCompat;

import com.android.server.display.DisplayControlHidden;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import dev.rikka.tools.refine.Refine;

public class HiddenApiUtil {
    private static final String XPOSED_METADATA = "xposedminversion";
    private static final String FLAG_USER = "user";
    private static final String FLAG_SYSTEM = "system";
    private static final String FLAG_XPOSED = "xposed";
    private static final String FORMAT_LABEL = "label";
    private static final String FORMAT_PKG_NAME = "pkgName";
    private static final String FORMAT_FLAG = "flag";

    private static void onHelp() {
        System.out.println("HiddenApiUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  getPackageUid USER_ID PACKAGE PACKAGE PACKAGE ...");
        System.out.println();
        System.out.println("  getPackageLabel USER_ID PACKAGE PACKAGE PACKAGE ...");
        System.out.println();
        System.out.println("  getPackageArchiveInfo APK_FILE");
        System.out.println();
        System.out.println("  getInstalledPackagesAsUser USER_ID FILTER_FLAG(user|system|xposed) FORMAT(label|pkgName|flag)");
        System.out.println();
        System.out.println("  getRuntimePermissions USER_ID PACKAGE PACKAGE PACKAGE ...");
        System.out.println();
        System.out.println("  grantRuntimePermission USER_ID [PACKAGE PERM_NAME PERM_NAME ...] [PACKAGE PERM_NAME ...] ...");
        System.out.println();
        System.out.println("  revokeRuntimePermission USER_ID [PACKAGE PERM_NAME PERM_NAME ...] [PACKAGE PERM_NAME ...] ...");
        System.out.println();
        System.out.println("  setOpsMode USER_ID [PACKAGE OP MODE OP MODE ...] [PACKAGE OP MODE ...] ...");
        System.out.println();
        System.out.println("  setDisplayPowerMode MODE(POWER_MODE_OFF: 0, POWER_MODE_NORMAL: 2)");
        System.out.println();
        System.out.println("  getNotificationSettings USER_ID PACKAGE PACKAGE PACKAGE ...");
        System.out.println();
        System.out.println("  setNotificationSettings USER_ID [PACKAGE KEY VALUE KEY VALUE ...] [PACKAGE KEY VALUE ...] ...");
        System.out.println();
        System.out.println("  getBatterySettings USER_ID PACKAGE PACKAGE PACKAGE ...");
        System.out.println();
        System.out.println("  setBatterySettings USER_ID [PACKAGE KEY VALUE KEY VALUE ...] [PACKAGE KEY VALUE ...] ...");
    }

    private static void onCommand(String cmd, String[] args) {
        switch (cmd) {
            case "getPackageUid":
                getPackageUid(args);
                break;
            case "getPackageLabel":
                getPackageLabel(args);
                break;
            case "getPackageArchiveInfo":
                getPackageArchiveInfo(args);
                break;
            case "getInstalledPackagesAsUser":
                getInstalledPackagesAsUser(args);
                break;
            case "getRuntimePermissions":
                getRuntimePermissions(args);
                break;
            case "getNotificationSettings":
                getNotificationSettings(args);
                break;
            case "setNotificationSettings":
                setNotificationSettings(args);
                break;
            case "getBatterySettings":
                getBatterySettings(args);
                break;
            case "setBatterySettings":
                setBatterySettings(args);
                break;
            case "grantRuntimePermission":
                grantRuntimePermission(args);
                break;
            case "revokeRuntimePermission":
                revokeRuntimePermission(args);
                break;
            case "setOpsMode":
                setOpsMode(args);
                break;
            case "setDisplayPowerMode":
                setDisplayPowerMode(args);
                break;
            case "help":
                onHelp();
                break;
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
            Context ctx = HiddenApiHelper.getContext();
            PackageManager pm = PackageManagerUtil.getPackageManager(ctx).packageManager();
            PackageManagerHidden pmHidden = Refine.unsafeCast(pm);
            int userId = Integer.parseInt(args[1]);
            for (int i = 2; i < args.length; i++) {
                String[] packageNames = args[i].split(" ");
                for (String packageName : packageNames) {
                    try {
                        PackageInfo packageInfo = pmHidden.getPackageInfoAsUser(packageName, 0, userId);
                        System.out.println(packageInfo.applicationInfo.uid);
                    } catch (Exception e) {
                        System.out.println("Failed, skipped: " + packageName);
                    }
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void getPackageLabel(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManager pm = PackageManagerUtil.getPackageManager(ctx).packageManager();
            PackageManagerHidden pmHidden = Refine.unsafeCast(pm);
            int userId = Integer.parseInt(args[1]);
            for (int i = 2; i < args.length; i++) {
                String[] packageNames = args[i].split(" ");
                for (String packageName : packageNames) {
                    try {
                        PackageInfo packageInfo = pmHidden.getPackageInfoAsUser(packageName, 0, userId);
                        System.out.println(removeSpaces(packageInfo.applicationInfo.loadLabel(pm).toString()));
                    } catch (Exception e) {
                        System.out.println("Failed, skipped: " + packageName);
                    }
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void getPackageArchiveInfo(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManager pm = PackageManagerUtil.getPackageManager(ctx).packageManager();
            String file = args[1];
            PackageInfo packageInfo = pm.getPackageArchiveInfo(file, 0);
            if (packageInfo != null && packageInfo.applicationInfo != null) {
                packageInfo.applicationInfo.sourceDir = file;
                packageInfo.applicationInfo.publicSourceDir = file;
                System.out.println(removeSpaces(packageInfo.applicationInfo.loadLabel(pm).toString()) + " " + packageInfo.packageName);
            } else {
                throw new PackageManager.NameNotFoundException("Failed to parse package info!");
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void getInstalledPackagesAsUser(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerUtil.PackageManagerWithLocale packageManagerWithLocale = PackageManagerUtil.getPackageManager(ctx);
            Locale locale = packageManagerWithLocale.locale();
            PackageManager pm = packageManagerWithLocale.packageManager();
            PackageManagerHidden pmHidden = Refine.unsafeCast(pm);
            int userId = Integer.parseInt(args[1]);
            List<String> filterFlags = new ArrayList<>();
            List<String> formatList = new ArrayList<>();
            try {
                filterFlags = Arrays.asList(args[2].split("\\|"));
            } catch (Exception ignored) {
            }
            if (filterFlags.isEmpty()) {
                filterFlags.add(FLAG_USER);
                filterFlags.add(FLAG_SYSTEM);
            }
            try {
                formatList = Arrays.asList(args[3].split("\\|"));
            } catch (Exception ignored) {
            }
            if (formatList.isEmpty()) {
                formatList.add(FORMAT_LABEL);
                formatList.add(FORMAT_PKG_NAME);
                formatList.add(FORMAT_FLAG);
            }
            boolean userFlag = filterFlags.contains(FLAG_USER);
            boolean systemFlag = filterFlags.contains(FLAG_SYSTEM);
            boolean xposedFlag = filterFlags.contains(FLAG_XPOSED);
            List<PackageInfo> packages = pmHidden.getInstalledPackagesAsUser(PackageManager.GET_META_DATA, userId);
            Collator collator = Collator.getInstance(locale != null ? locale : ctx.getResources().getConfiguration().getLocales().get(0));
            packages.sort((p1, p2) -> {
                if (p1 != null && p2 != null) {
                    return collator.getCollationKey(p1.applicationInfo.loadLabel(pm).toString())
                            .compareTo(collator.getCollationKey(p2.applicationInfo.loadLabel(pm).toString()));
                }
                return 0;
            });
            for (PackageInfo pkg : packages) {
                boolean isSystemApp = (pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isUserApp = !isSystemApp;
                boolean isXposedApp = ((pkg.applicationInfo.metaData != null && pkg.applicationInfo.metaData.containsKey(XPOSED_METADATA))
                        || isModernModules(pkg.applicationInfo));
                if ((userFlag && isUserApp) || (systemFlag && isSystemApp) || (xposedFlag && isXposedApp)) {
                    StringBuilder out = new StringBuilder();
                    for (String format : formatList) {
                        switch (format) {
                            case FORMAT_LABEL ->
                                    out.append(" ").append(removeSpaces(pkg.applicationInfo.loadLabel(pm).toString().replaceAll("\n", "")));
                            case FORMAT_PKG_NAME -> out.append(" ").append(pkg.packageName);
                            case FORMAT_FLAG -> {
                                List<String> flags = new ArrayList<>();
                                if (isUserApp) {
                                    flags.add(FLAG_USER);
                                }
                                if (isSystemApp) {
                                    flags.add(FLAG_SYSTEM);
                                }
                                if (isXposedApp) {
                                    flags.add(FLAG_XPOSED);
                                }
                                out.append(" ").append(String.join("|", flags));
                            }
                        }
                    }
                    String item = out.toString().trim();
                    if (!item.isEmpty()) {
                        System.out.println(item);
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
    private static void getRuntimePermissions(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManager packageManager = PackageManagerUtil.getPackageManager(ctx).packageManager();
            PackageManagerHidden packageManagerHidden = Refine.unsafeCast(packageManager);
            AppOpsManagerHidden appOpsManager = (AppOpsManagerHidden) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int userId = Integer.parseInt(args[1]);
            List<String> packageNames = collectPackageNames(args, 2);
            for (String packageName : packageNames) {
                try {
                    printRuntimePermissions(packageManager, packageManagerHidden, appOpsManager, userId, packageName);
                } catch (Exception e) {
                    System.out.println("Failed, skipped: " + packageName);
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void printRuntimePermissions(
            PackageManager packageManager,
            PackageManagerHidden packageManagerHidden,
            AppOpsManagerHidden appOpsManager,
            int userId,
            String packageName
    ) {
        PackageInfo packageInfo = packageManagerHidden.getPackageInfoAsUser(packageName, PackageManager.GET_PERMISSIONS, userId);
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
        if (requestedPermissions != null && requestedPermissionsFlags != null) {
            for (int i = 0; i < requestedPermissions.length; i++) {
                try {
                    PermissionInfo permissionInfo = packageManager.getPermissionInfo(requestedPermissions[i], 0);
                    int protection = PermissionInfoCompat.getProtection(permissionInfo);
                    int protectionFlags = PermissionInfoCompat.getProtectionFlags(permissionInfo);
                    boolean isGranted = (requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                    int op = AppOpsManagerHidden.permissionToOpCode(requestedPermissions[i]);
                    int mode = AppOpsManagerHidden.MODE_IGNORED;
                    if (opsMap != null && opsMap.containsKey(op)) {
                        mode = getOpMode(appOpsManager, op, packageInfo.applicationInfo.uid, packageName);
                        opsMap.remove(op);
                    }
                    if ((op != AppOpsManagerHidden.OP_NONE)
                            || (protection == PermissionInfo.PROTECTION_DANGEROUS || (protectionFlags & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0)) {
                        System.out.println(formatRuntimePermissionLine(packageName, requestedPermissions[i], isGranted, op, mode));
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }
        if (opsMap == null) {
            return;
        }
        for (Map.Entry<Integer, Integer> entry : opsMap.entrySet()) {
            int op = entry.getKey();
            int mode = entry.getValue();
            String publicName = getPublicName(op);
            System.out.println(formatRuntimePermissionLine(packageName, publicName, isModeAllowed(mode), op, mode));
        }
    }

    private static String getPublicName(int op) {
        try {
            String publicName = AppOpsManagerHidden.opToPublicName(op);
            if (publicName != null && !publicName.isEmpty()) {
                return publicName;
            }
        } catch (Throwable ignored) {
        }
        try {
            String name = AppOpsManagerHidden.opToName(op);
            if (name != null && !name.isEmpty()) {
                return "android:" + name.toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {
        }
        return "android:op_" + op;
    }

    @SuppressLint("ServiceCast")
    private static void grantRuntimePermission(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden pm = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            int userId = Integer.parseInt(args[1]);
            UserHandle user = UserHandleHidden.of(userId);
            for (PackagePermissionSet permissionSet : parsePackagePermissionSets(args, 2)) {
                for (String permName : permissionSet.permissionNames) {
                    try {
                        pm.grantRuntimePermission(permissionSet.packageName, permName, user);
                    } catch (Exception e) {
                        System.out.println("Failed, skipped: " + permissionSet.packageName + " " + permName);
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
    private static void revokeRuntimePermission(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden pm = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            int userId = Integer.parseInt(args[1]);
            UserHandle user = UserHandleHidden.of(userId);
            for (PackagePermissionSet permissionSet : parsePackagePermissionSets(args, 2)) {
                for (String permName : permissionSet.permissionNames) {
                    try {
                        pm.revokeRuntimePermission(permissionSet.packageName, permName, user);
                    } catch (Exception e) {
                        System.out.println("Failed, skipped: " + permissionSet.packageName + " " + permName);
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
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden packageManager = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            AppOpsManagerHidden appOpsManager = (AppOpsManagerHidden) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int userId = Integer.parseInt(args[1]);
            for (PackageOpModeSet opModeSet : parsePackageOpModeSets(args, 2)) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfoAsUser(opModeSet.packageName, PackageManager.GET_PERMISSIONS, userId);
                    for (OpMode opMode : opModeSet.opModes) {
                        try {
                            appOpsManager.setMode(opMode.op, packageInfo.applicationInfo.uid, opModeSet.packageName, opMode.mode);
                        } catch (Exception e) {
                            System.out.println("Failed, skipped: " + opModeSet.packageName + " " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed, skipped: " + opModeSet.packageName + " " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    public static void setDisplayPowerMode(String[] args) {
        try {
            int mode = Integer.parseInt(args[1]);
            long[] physicalDisplayIds;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                physicalDisplayIds = DisplayControlHidden.getPhysicalDisplayIds();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                physicalDisplayIds = SurfaceControlHidden.getPhysicalDisplayIds();
            } else {
                physicalDisplayIds = new long[]{0L};
            }
            for (long id : physicalDisplayIds) {
                IBinder token;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    token = DisplayControlHidden.getPhysicalDisplayToken(id);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    token = SurfaceControlHidden.getPhysicalDisplayToken(id);
                } else {
                    token = SurfaceControlHidden.getBuiltInDisplay((int) id);
                }
                SurfaceControlHidden.setDisplayPowerMode(token, mode);
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static String removeSpaces(String string) {
        return string.replaceAll("\\s", "");
    }

    /**
     * @see <a href="https://github.com/LSPosed/LSPosed/blob/df74d83eb03a44cc6ad268841ac2ada28d077c77/daemon/src/main/java/org/lsposed/lspd/service/LSPosedService.java#L69">LSPosedService.java#L69</a>
     */
    private static boolean isModernModules(ApplicationInfo info) {
        String[] apks;
        if (info.splitSourceDirs != null) {
            apks = Arrays.copyOf(info.splitSourceDirs, info.splitSourceDirs.length + 1);
            apks[info.splitSourceDirs.length] = info.sourceDir;
        } else apks = new String[]{info.sourceDir};
        for (var apk : apks) {
            try (var zip = new ZipFile(apk)) {
                if (zip.getEntry("META-INF/xposed/java_init.list") != null) {
                    return true;
                }
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    private static List<String> collectPackageNames(String[] args, int start) {
        return flattenArgs(args, start);
    }

    private static String formatRuntimePermissionLine(String packageName, String permissionName, boolean isGranted, int op, int mode) {
        return packageName + " " + permissionName + " " + isGranted + " " + op + " " + mode;
    }

    private static boolean isModeAllowed(int mode) {
        return mode == AppOpsManagerHidden.MODE_ALLOWED || mode == AppOpsManagerHidden.MODE_FOREGROUND;
    }

    @SuppressLint("ServiceCast")
    private static void getBatterySettings(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden packageManager = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            AppOpsManagerHidden appOpsManager = (AppOpsManagerHidden) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int userId = Integer.parseInt(args[1]);
            Set<String> idleWhitelist = getDeviceIdleWhitelist();
            List<String> packageNames = collectPackageNames(args, 2);
            for (String packageName : packageNames) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfoAsUser(packageName, 0, userId);
                    int uid = packageInfo.applicationInfo.uid;
                    printBatteryOp(appOpsManager, packageName, uid, "RUN_IN_BACKGROUND");
                    printBatteryOp(appOpsManager, packageName, uid, "RUN_ANY_IN_BACKGROUND");
                    System.out.println(packageName + " BATTERY:deviceidle_whitelist " + idleWhitelist.contains(packageName));
                } catch (Exception e) {
                    System.out.println("Failed, skipped: " + packageName + " " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    @SuppressLint("ServiceCast")
    private static void setBatterySettings(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden packageManager = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            AppOpsManagerHidden appOpsManager = (AppOpsManagerHidden) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int userId = Integer.parseInt(args[1]);
            for (PackageNotificationSettingSet set : parsePackageNotificationSettingSets(args, 2)) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfoAsUser(set.packageName, 0, userId);
                    int uid = packageInfo.applicationInfo.uid;
                    for (NotificationSettingValue item : set.items) {
                        try {
                            applyBatterySetting(appOpsManager, set.packageName, uid, item.key, item.value);
                        } catch (Exception e) {
                            System.out.println("Failed, skipped: " + set.packageName + " " + item.key + " " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed, skipped: " + set.packageName + " " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void printBatteryOp(AppOpsManagerHidden appOpsManager, String packageName, int uid, String opName) {
        int op = resolveBatteryOp(opName);
        if (op == AppOpsManagerHidden.OP_NONE) {
            System.out.println(packageName + " BATTERY:" + opName + " -1 " + AppOpsManagerHidden.MODE_IGNORED + " ignored");
            return;
        }
        int mode = getOpMode(appOpsManager, op, uid, packageName);
        System.out.println(packageName + " BATTERY:" + opName + " " + op + " " + mode + " " + appOpsModeToName(mode));
    }

    private static int getOpMode(AppOpsManagerHidden appOpsManager, int op, int uid, String packageName) {
        if (op == AppOpsManagerHidden.OP_NONE) {
            return AppOpsManagerHidden.MODE_IGNORED;
        }
        try {
            return appOpsManager.unsafeCheckOpRawNoThrow(op, uid, packageName);
        } catch (Throwable ignored) {
        }
        try {
            return appOpsManager.checkOpNoThrow(op, uid, packageName);
        } catch (Throwable ignored) {
        }
        return AppOpsManagerHidden.MODE_IGNORED;
    }

    private static void applyBatterySetting(AppOpsManagerHidden appOpsManager, String packageName, int uid, String key, String value) throws Exception {
        if ("battery_opt".equals(key) || "BATTERY:RUN_ANY_IN_BACKGROUND".equals(key)) {
            int op = resolveBatteryOp("RUN_ANY_IN_BACKGROUND");
            if (op == AppOpsManagerHidden.OP_NONE)
                throw new IllegalArgumentException("Not found RUN_ANY_IN_BACKGROUND AppOp");
            int mode = parseBatteryMode(value);
            appOpsManager.setMode(op, uid, packageName, mode);
            if (mode == AppOpsManagerHidden.MODE_ALLOWED) {
                setDeviceIdleWhitelist(packageName, true);
            }
            return;
        }
        if ("BATTERY:RUN_IN_BACKGROUND".equals(key)) {
            int op = resolveBatteryOp("RUN_IN_BACKGROUND");
            if (op == AppOpsManagerHidden.OP_NONE)
                throw new IllegalArgumentException("Not found RUN_IN_BACKGROUND AppOp");
            appOpsManager.setMode(op, uid, packageName, parseBatteryMode(value));
            return;
        }
        if ("BATTERY:deviceidle_whitelist".equals(key) || "BATTERY:idle_whitelist".equals(key) || "BATTERY:doze_whitelist".equals(key)) {
            setDeviceIdleWhitelist(packageName, Boolean.parseBoolean(value));
            return;
        }
        throw new IllegalArgumentException("Unknownbatterysetting key: " + key);
    }

    private static int parseBatteryMode(String raw) {
        if (raw == null) return AppOpsManagerHidden.MODE_DEFAULT;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.contains(" ")) {
            String[] parts = v.split("\\s+");
            if (parts.length >= 2) v = parts[1];
        }
        switch (v) {
            case "allow":
            case "allowed":
            case "true":
                return AppOpsManagerHidden.MODE_ALLOWED;
            case "ignore":
            case "ignored":
            case "false":
                return AppOpsManagerHidden.MODE_IGNORED;
            case "deny":
            case "denied":
            case "errored":
                return AppOpsManagerHidden.MODE_ERRORED;
            case "default":
                return AppOpsManagerHidden.MODE_DEFAULT;
            case "foreground":
                return AppOpsManagerHidden.MODE_FOREGROUND;
            default:
                return Integer.parseInt(v);
        }
    }

    private static String appOpsModeToName(int mode) {
        switch (mode) {
            case AppOpsManagerHidden.MODE_ALLOWED:
                return "allow";
            case AppOpsManagerHidden.MODE_IGNORED:
                return "ignore";
            case AppOpsManagerHidden.MODE_ERRORED:
                return "deny";
            case AppOpsManagerHidden.MODE_DEFAULT:
                return "default";
            case AppOpsManagerHidden.MODE_FOREGROUND:
                return "foreground";
            default:
                return String.valueOf(mode);
        }
    }

    private static int resolveBatteryOp(String opName) {
        String publicName = "android:" + opName.toLowerCase(Locale.ROOT);
        try {
            int op = AppOpsManagerHidden.strOpToOp(publicName);
            if (op != AppOpsManagerHidden.OP_NONE) return op;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> clazz = Class.forName("android.app.AppOpsManager");
            java.lang.reflect.Field field = clazz.getDeclaredField("OP_" + opName);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Throwable ignored) {
        }
        return AppOpsManagerHidden.OP_NONE;
    }

    private static Object getDeviceIdleService() throws Exception {
        Object binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class)
                .invoke(null, "deviceidle");
        return Class.forName("android.os.IDeviceIdleController$Stub")
                .getMethod("asInterface", android.os.IBinder.class)
                .invoke(null, binder);
    }

    private static Set<String> getDeviceIdleWhitelist() {
        Set<String> result = new HashSet<>();
        try {
            Object service = getDeviceIdleService();
            // Compatibility / fallback handling.
            Object names = callRequired(service,
                    new CallSpec("getFullPowerWhitelist"),
                    new CallSpec("getFullPowerWhitelistExceptIdle")
            );
            if (names instanceof String[]) {
                for (String name : (String[]) names) {
                    if (name != null && !name.isEmpty()) result.add(name);
                }
                return result;
            }
        } catch (Throwable ignored) {
            // Compatibility / fallback handling.
        }
        return getDeviceIdleWhitelistViaDumpsys();
    }

    private static void setDeviceIdleWhitelist(String packageName, boolean enabled) {
        try {
            Object service = getDeviceIdleService();
            if (enabled) {
                callRequired(service, new CallSpec("addPowerSaveWhitelistApp", packageName));
            } else {
                callRequired(service, new CallSpec("removePowerSaveWhitelistApp", packageName));
            }
            return;
        } catch (Throwable ignored) {
            // Compatibility / fallback handling.
        }
        setDeviceIdleWhitelistViaDumpsys(packageName, enabled);
    }

    // Compatibility / fallback handling.
    private static Set<String> getDeviceIdleWhitelistViaDumpsys() {
        Set<String> result = new HashSet<>();
        String output = execShellCapture("dumpsys deviceidle whitelist");
        for (String line : output.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("Added:") || line.startsWith("Removed:")) {
                int colon = line.indexOf(':');
                if (colon >= 0 && colon + 1 < line.length()) {
                    String pkg = line.substring(colon + 1).trim();
                    if (pkg.contains(".")) result.add(pkg);
                }
                continue;
            }
            if (line.contains(",")) {
                String[] commaParts = line.split(",");
                for (String part : commaParts) {
                    String pkg = part.trim();
                    if (pkg.contains(".") && !pkg.matches("^[0-9]+$")) {
                        result.add(pkg);
                    }
                }
                continue;
            }
            String[] parts = line.split("\\s+");
            for (String part : parts) {
                String pkg = part.trim();
                if (pkg.contains(".")) result.add(pkg);
            }
        }
        return result;
    }

    private static void setDeviceIdleWhitelistViaDumpsys(String packageName, boolean enabled) {
        String safePkg = packageName.replaceAll("[^A-Za-z0-9._-]", "");
        if (safePkg.isEmpty()) return;
        String prefix = enabled ? "+" : "-";

        // Compatibility / fallback handling.
        if (execShellSuccess("cmd deviceidle whitelist " + prefix + safePkg)) {
            return;
        }
        execShellCapture("dumpsys deviceidle whitelist " + prefix + safePkg);
    }

    private static boolean execShellSuccess(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                while (br.readLine() != null) {
                }
            }
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
                while (br.readLine() != null) {
                }
            }
            return process.waitFor() == 0;
        } catch (Throwable ignored) {
            return false;
        }
    }


    private static String execShellCapture(String cmd) {
        StringBuilder sb = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
            }
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
                while (br.readLine() != null) {
                }
            }
            process.waitFor();
        } catch (Throwable ignored) {
        }
        return sb.toString();
    }


    private static void getNotificationSettings(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden packageManager = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            int userId = Integer.parseInt(args[1]);
            Object notificationManager = getNotificationService();
            List<String> packageNames = collectPackageNames(args, 2);
            for (String packageName : packageNames) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfoAsUser(packageName, 0, userId);
                    int uid = packageInfo.applicationInfo.uid;
                    printNotificationSettings(notificationManager, packageName, uid);
                } catch (Exception e) {
                    System.out.println("Failed, skipped: " + packageName + " " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void setNotificationSettings(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden packageManager = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            int userId = Integer.parseInt(args[1]);
            Object notificationManager = getNotificationService();
            for (PackageNotificationSettingSet set : parsePackageNotificationSettingSets(args, 2)) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfoAsUser(set.packageName, 0, userId);
                    int uid = packageInfo.applicationInfo.uid;
                    for (NotificationSettingValue item : set.items) {
                        try {
                            applyNotificationSetting(notificationManager, set.packageName, uid, item.key, item.value);
                        } catch (Exception e) {
                            System.out.println("Failed, skipped: " + set.packageName + " " + item.key + " " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed, skipped: " + set.packageName + " " + e.getMessage());
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static Object getNotificationService() throws Exception {
        Object binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class)
                .invoke(null, "notification");
        return Class.forName("android.app.INotificationManager$Stub")
                .getMethod("asInterface", android.os.IBinder.class)
                .invoke(null, binder);
    }

    private static void printNotificationSettings(Object notificationManager, String packageName, int uid) {
        printNotificationValue(packageName, "NOTIFY_APP:enabled", safeBoolean(callFirst(notificationManager,
                new CallSpec("areNotificationsEnabledForPackage", packageName, uid),
                new CallSpec("areNotificationsEnabled", packageName)
        ), true));
        Object importance = callFirst(notificationManager,
                new CallSpec("getPackageImportance", packageName),
                new CallSpec("getImportance", packageName),
                new CallSpec("getPackageImportance", packageName, uid)
        );
        if (importance instanceof Number) {
            printNotificationValue(packageName, "NOTIFY_APP:importance", String.valueOf(((Number) importance).intValue()));
        }

        Object appShowBadge = callFirst(notificationManager,
                new CallSpec("canShowBadge", packageName, uid),
                new CallSpec("getShowBadge", packageName, uid)
        );
        if (appShowBadge instanceof Boolean) {
            printNotificationValue(packageName, "NOTIFY_APP:showBadge", String.valueOf(appShowBadge));
        }

        Object appBubblePreference = callFirst(notificationManager,
                new CallSpec("getBubblePreferenceForPackage", packageName, uid),
                new CallSpec("getBubblesAllowed", packageName, uid),
                new CallSpec("getBubblePreference", packageName, uid)
        );
        if (appBubblePreference instanceof Number) {
            printNotificationValue(packageName, "NOTIFY_APP:bubblePreference", String.valueOf(((Number) appBubblePreference).intValue()));
        } else if (appBubblePreference instanceof Boolean) {
            printNotificationValue(packageName, "NOTIFY_APP:allowBubbles", String.valueOf(appBubblePreference));
        }

        for (Object group : getNotificationGroups(notificationManager, packageName, uid)) {
            String groupId = safeString(invokeNoArg(group, "getId"));
            if (groupId == null || groupId.isEmpty()) {
                continue;
            }
            String keyPrefix = "NOTIFY_GROUP:" + encodeToken(groupId) + ":";
            Object blocked = invokeNoArg(group, "isBlocked");
            if (blocked instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "blocked", String.valueOf(blocked));
            }
        }

        for (Object channel : getNotificationChannels(notificationManager, packageName, uid)) {
            String channelId = safeString(invokeNoArg(channel, "getId"));
            if (channelId == null || channelId.isEmpty()) {
                continue;
            }
            String keyPrefix = "NOTIFY_CHANNEL:" + encodeToken(channelId) + ":";
            Object importanceValue = invokeNoArg(channel, "getImportance");
            if (importanceValue instanceof Number) {
                printNotificationValue(packageName, keyPrefix + "importance", String.valueOf(((Number) importanceValue).intValue()));
            }
            Object showBadge = invokeNoArg(channel, "canShowBadge");
            if (showBadge instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "showBadge", String.valueOf(showBadge));
            }
            Object allowBubbles = callFirst(channel,
                    new CallSpec("getAllowBubbles"),
                    new CallSpec("canBubble")
            );
            if (allowBubbles instanceof Number) {
                printNotificationValue(packageName, keyPrefix + "allowBubbles", String.valueOf(((Number) allowBubbles).intValue()));
            } else if (allowBubbles instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "canBubble", String.valueOf(allowBubbles));
            }
            Object importantConversation = invokeNoArg(channel, "isImportantConversation");
            if (importantConversation instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "importantConversation", String.valueOf(importantConversation));
            }
            Object demoted = invokeNoArg(channel, "isDemoted");
            if (demoted instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "demoted", String.valueOf(demoted));
            }
            Object shouldVibrate = invokeNoArg(channel, "shouldVibrate");
            if (shouldVibrate instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "vibration", String.valueOf(shouldVibrate));
            }
            Object shouldShowLights = invokeNoArg(channel, "shouldShowLights");
            if (shouldShowLights instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "lights", String.valueOf(shouldShowLights));
            }
            Object deleted = invokeNoArg(channel, "isDeleted");
            if (deleted instanceof Boolean) {
                printNotificationValue(packageName, keyPrefix + "deleted", String.valueOf(deleted));
            }
        }
    }

    private static void printNotificationValue(String packageName, String key, String value) {
        if (value != null) {
            System.out.println(packageName + " " + key + " " + value);
        }
    }

    private static List<Object> getNotificationChannels(Object notificationManager, String packageName, int uid) {
        Object slice = callFirst(notificationManager,
                new CallSpec("getNotificationChannelsForPackage", packageName, uid, true),
                new CallSpec("getNotificationChannelsForPackage", packageName, uid, true, false),
                new CallSpec("getNotificationChannels", packageName, uid, true)
        );
        return listFromSliceOrList(slice);
    }

    private static List<Object> getNotificationGroups(Object notificationManager, String packageName, int uid) {
        Object slice = callFirst(notificationManager,
                new CallSpec("getNotificationChannelGroupsForPackage", packageName, uid, true),
                new CallSpec("getNotificationChannelGroupsForPackage", packageName, uid, true, false),
                new CallSpec("getNotificationChannelGroups", packageName, uid, true)
        );
        return listFromSliceOrList(slice);
    }

    private static List<Object> listFromSliceOrList(Object obj) {
        List<Object> result = new ArrayList<>();
        if (obj == null) {
            return result;
        }
        try {
            Object list = obj;
            if (!(list instanceof List)) {
                list = invokeNoArg(obj, "getList");
            }
            if (list instanceof List<?>) {
                result.addAll((List<?>) list);
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static void applyNotificationSetting(Object notificationManager, String packageName, int uid, String key, String value) throws Exception {
        if (key == null || value == null) {
            return;
        }
        if ("NOTIFY_APP:enabled".equals(key)) {
            boolean enabled = Boolean.parseBoolean(value);
            callRequired(notificationManager,
                    new CallSpec("setNotificationsEnabledWithImportanceLockForPackage", packageName, uid, enabled),
                    new CallSpec("setNotificationsEnabledForPackage", packageName, uid, enabled),
                    new CallSpec("setNotificationsEnabled", packageName, enabled)
            );
            return;
        }
        if ("NOTIFY_APP:showBadge".equals(key)) {
            boolean showBadge = Boolean.parseBoolean(value);
            callRequired(notificationManager,
                    new CallSpec("setShowBadge", packageName, uid, showBadge)
            );
            return;
        }
        if ("NOTIFY_APP:bubblePreference".equals(key)) {
            int bubblePreference = Integer.parseInt(value);
            callRequired(notificationManager,
                    new CallSpec("setBubblesAllowed", packageName, uid, bubblePreference),
                    new CallSpec("setBubblePreferenceForPackage", packageName, uid, bubblePreference)
            );
            return;
        }
        if ("NOTIFY_APP:allowBubbles".equals(key)) {
            int bubblePreference = Boolean.parseBoolean(value) ? 1 : 0;
            callRequired(notificationManager,
                    new CallSpec("setBubblesAllowed", packageName, uid, bubblePreference),
                    new CallSpec("setBubblePreferenceForPackage", packageName, uid, bubblePreference)
            );
            return;
        }
        if (key.startsWith("NOTIFY_CHANNEL:")) {
            String[] parts = key.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("notification channel key Invalid format: " + key);
            }
            String channelId = decodeToken(parts[1]);
            String field = parts[2];
            Object channel = findNotificationChannel(notificationManager, packageName, uid, channelId);
            if (channel == null) {
                throw new IllegalArgumentException("Not foundnotification channel: " + channelId);
            }
            applyChannelField(channel, field, value);
            callRequired(notificationManager,
                    new CallSpec("updateNotificationChannelForPackage", packageName, uid, channel)
            );
            return;
        }
        if (key.startsWith("NOTIFY_GROUP:")) {
            String[] parts = key.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("notification channel group key Invalid format: " + key);
            }
            String groupId = decodeToken(parts[1]);
            String field = parts[2];
            Object group = findNotificationGroup(notificationManager, packageName, uid, groupId);
            if (group == null) {
                throw new IllegalArgumentException("Not foundnotification channel group: " + groupId);
            }
            applyGroupField(group, field, value);
            callRequired(notificationManager,
                    new CallSpec("updateNotificationChannelGroupForPackage", packageName, uid, group)
            );
            return;
        }
        throw new IllegalArgumentException("Unknown notification setting key: " + key);
    }

    private static Object findNotificationChannel(Object notificationManager, String packageName, int uid, String channelId) {
        for (Object channel : getNotificationChannels(notificationManager, packageName, uid)) {
            if (channelId.equals(safeString(invokeNoArg(channel, "getId")))) {
                return channel;
            }
        }
        return null;
    }

    private static Object findNotificationGroup(Object notificationManager, String packageName, int uid, String groupId) {
        for (Object group : getNotificationGroups(notificationManager, packageName, uid)) {
            if (groupId.equals(safeString(invokeNoArg(group, "getId")))) {
                return group;
            }
        }
        return null;
    }

    private static void applyChannelField(Object channel, String field, String value) throws Exception {
        switch (field) {
            case "importance":
                invokeRequired(channel, "setImportance", Integer.parseInt(value));
                break;
            case "showBadge":
                invokeRequired(channel, "setShowBadge", Boolean.parseBoolean(value));
                break;
            case "allowBubbles":
                invokeRequired(channel, "setAllowBubbles", Integer.parseInt(value));
                break;
            case "canBubble":
                invokeRequired(channel, "setAllowBubbles", Boolean.parseBoolean(value) ? 1 : 0);
                break;
            case "importantConversation":
                invokeRequired(channel, "setImportantConversation", Boolean.parseBoolean(value));
                break;
            case "demoted":
                invokeRequired(channel, "setDemoted", Boolean.parseBoolean(value));
                break;
            case "vibration":
                invokeRequired(channel, "enableVibration", Boolean.parseBoolean(value));
                break;
            case "lights":
                invokeRequired(channel, "enableLights", Boolean.parseBoolean(value));
                break;
            default:
                throw new IllegalArgumentException("Unsupported notification channelfield: " + field);
        }
    }

    private static void applyGroupField(Object group, String field, String value) throws Exception {
        if ("blocked".equals(field)) {
            invokeRequired(group, "setBlocked", Boolean.parseBoolean(value));
        } else {
            throw new IllegalArgumentException("Unsupported notification channel groupfield: " + field);
        }
    }

    private static Object callFirst(Object target, CallSpec... specs) {
        for (CallSpec spec : specs) {
            try {
                return invokeFlexible(target, spec.methodName, spec.args);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object callRequired(Object target, CallSpec... specs) throws Exception {
        Throwable last = null;
        for (CallSpec spec : specs) {
            try {
                return invokeFlexible(target, spec.methodName, spec.args);
            } catch (Throwable e) {
                last = e;
            }
        }
        throw new IllegalStateException(last != null ? last.getMessage() : "No matching method found");
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            return invokeFlexible(target, methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeRequired(Object target, String methodName, Object... args) throws Exception {
        return invokeFlexible(target, methodName, args);
    }

    private static Object invokeFlexible(Object target, String methodName, Object... args) throws Exception {
        Class<?> clazz = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        java.lang.reflect.Method[] methods = clazz.getMethods();
        for (java.lang.reflect.Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) {
                continue;
            }
            if (!areArgsCompatible(paramTypes, args)) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        }
        methods = clazz.getDeclaredMethods();
        for (java.lang.reflect.Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) {
                continue;
            }
            if (!areArgsCompatible(paramTypes, args)) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName);
    }

    private static boolean areArgsCompatible(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> wrapper = primitiveToWrapper(paramTypes[i]);
            if (!wrapper.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> primitiveToWrapper(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return Void.class;
    }

    private static String safeBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        return String.valueOf(fallback);
    }

    private static String safeString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String encodeToken(String raw) {
        try {
            return android.util.Base64.encodeToString(raw.getBytes("UTF-8"), android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
        } catch (Throwable ignored) {
            return raw.replace("%", "%25").replace(":", "%3A").replace(" ", "%20");
        }
    }

    private static String decodeToken(String token) {
        try {
            return new String(android.util.Base64.decode(token, android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING), "UTF-8");
        } catch (Throwable ignored) {
            return token.replace("%20", " ").replace("%3A", ":").replace("%25", "%");
        }
    }

    private static List<PackageNotificationSettingSet> parsePackageNotificationSettingSets(String[] args, int start) {
        List<PackageNotificationSettingSet> sets = new ArrayList<>();
        for (List<String> tokens : parseBracketGroups(args, start)) {
            PackageNotificationSettingSet set = new PackageNotificationSettingSet(tokens.get(0));
            for (int j = 1; j < tokens.size(); j += 2) {
                if (j + 1 >= tokens.size()) {
                    throw new IllegalArgumentException("Missing notification setting value，key: " + tokens.get(j));
                }
                set.items.add(new NotificationSettingValue(tokens.get(j), tokens.get(j + 1)));
            }
            sets.add(set);
        }
        return sets;
    }

    private static final class CallSpec {
        final String methodName;
        final Object[] args;

        CallSpec(String methodName, Object... args) {
            this.methodName = methodName;
            this.args = args;
        }
    }

    private static final class PackageNotificationSettingSet {
        final String packageName;
        final List<NotificationSettingValue> items = new ArrayList<>();

        PackageNotificationSettingSet(String packageName) {
            this.packageName = packageName;
        }
    }

    private static final class NotificationSettingValue {
        final String key;
        final String value;

        NotificationSettingValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }


    private static List<PackagePermissionSet> parsePackagePermissionSets(String[] args, int start) {
        List<PackagePermissionSet> sets = new ArrayList<>();
        for (List<String> tokens : parseBracketGroups(args, start)) {
            PackagePermissionSet set = new PackagePermissionSet(tokens.get(0));
            set.permissionNames.addAll(tokens.subList(1, tokens.size()));
            sets.add(set);
        }
        return sets;
    }

    private static List<PackageOpModeSet> parsePackageOpModeSets(String[] args, int start) {
        List<PackageOpModeSet> sets = new ArrayList<>();
        for (List<String> tokens : parseBracketGroups(args, start)) {
            PackageOpModeSet set = new PackageOpModeSet(tokens.get(0));
            for (int j = 1; j < tokens.size(); j += 2) {
                if (j + 1 >= tokens.size()) {
                    throw new IllegalArgumentException("Missing AppOps mode: " + tokens.get(j));
                }
                set.opModes.add(new OpMode(Integer.parseInt(tokens.get(j)), Integer.parseInt(tokens.get(j + 1))));
            }
            sets.add(set);
        }
        return sets;
    }

    private static List<String> flattenArgs(String[] args, int start) {
        List<String> tokens = new ArrayList<>();
        for (int i = start; i < args.length; i++) {
            for (String token : args[i].trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private static List<List<String>> parseBracketGroups(String[] args, int start) {
        List<List<String>> groups = new ArrayList<>();
        List<String> current = null;
        for (int i = start; i < args.length; i++) {
            for (String rawToken : args[i].trim().split("\\s+")) {
                if (rawToken.isEmpty()) {
                    continue;
                }
                boolean startsGroup = rawToken.startsWith("[");
                boolean endsGroup = rawToken.endsWith("]");
                if (startsGroup) {
                    if (current != null) {
                        throw new IllegalArgumentException("Nested groups are not supported: " + rawToken);
                    }
                    current = new ArrayList<>();
                    rawToken = rawToken.substring(1);
                }
                if (current == null) {
                    throw new IllegalArgumentException("Missing group start marker [: " + rawToken);
                }
                if (endsGroup) {
                    rawToken = rawToken.substring(0, rawToken.length() - 1);
                }
                if (!rawToken.isEmpty()) {
                    current.add(rawToken);
                }
                if (endsGroup) {
                    if (current.isEmpty()) {
                        throw new IllegalArgumentException("Empty group");
                    }
                    groups.add(current);
                    current = null;
                }
            }
        }
        if (current != null) {
            throw new IllegalArgumentException("Missing group end marker ]");
        }
        return groups;
    }

    private static final class SpecialAppOp {
        final String publicName;
        final String fieldName;
        final String permissionName;
        final boolean requirePictureInPictureActivity;

        SpecialAppOp(String publicName, String fieldName, String permissionName, boolean requirePictureInPictureActivity) {
            this.publicName = publicName;
            this.fieldName = fieldName;
            this.permissionName = permissionName;
            this.requirePictureInPictureActivity = requirePictureInPictureActivity;
        }
    }

    private static final class PackagePermissionSet {
        final String packageName;
        final List<String> permissionNames = new ArrayList<>();

        PackagePermissionSet(String packageName) {
            this.packageName = packageName;
        }
    }

    private static final class PackageOpModeSet {
        final String packageName;
        final List<OpMode> opModes = new ArrayList<>();

        PackageOpModeSet(String packageName) {
            this.packageName = packageName;
        }
    }

    private static final class OpMode {
        final int op;
        final int mode;

        OpMode(int op, int mode) {
            this.op = op;
            this.mode = mode;
        }
    }
}
