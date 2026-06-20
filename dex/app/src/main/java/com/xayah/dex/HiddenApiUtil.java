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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        System.out.println("  grantRuntimePermission USER_ID [PACKAGE PERM_NAME PERM_NAME ...] [PACKAGE PERM_NAME ...]");
        System.out.println();
        System.out.println("  revokeRuntimePermission USER_ID [PACKAGE PERM_NAME PERM_NAME ...] [PACKAGE PERM_NAME ...]");
        System.out.println();
        System.out.println("  setOpsMode USER_ID [PACKAGE OP MODE OP MODE ...] [PACKAGE OP MODE ...]");
        System.out.println();
        System.out.println("  setDisplayPowerMode MODE(POWER_MODE_OFF: 0, POWER_MODE_NORMAL: 2)");
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
                        System.out.println("Failed, skip: " + packageName);
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
                        System.out.println("Failed, skip: " + packageName);
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
                                if (filterFlags.size() == 2) {
                                    if (systemFlag && xposedFlag) {
                                        if (isXposedApp) {
                                            out.append(" ").append(FLAG_XPOSED);
                                        }
                                    } else {
                                        if (xposedFlag && isXposedApp) {
                                            out.append(" ").append(FLAG_XPOSED);
                                        } else if (systemFlag && isSystemApp) {
                                            out.append(" ").append(FLAG_SYSTEM);
                                        }
                                    }
                                } else if (filterFlags.size() == 3) {
                                    List<String> flags = new ArrayList<>();
                                    if (isXposedApp) {
                                        flags.add(FLAG_XPOSED);
                                    }
                                    if (isSystemApp) {
                                        flags.add(FLAG_SYSTEM);
                                    }
                                    out.append(" ").append(String.join("|", flags));
                                }
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
                    System.out.println("Failed, skip: " + packageName);
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
                        if (opsMap != null) {
                            mode = opsMap.getOrDefault(op, AppOpsManagerHidden.MODE_IGNORED);
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
    }

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
                        System.out.println("Failed, skip: " + permissionSet.packageName + " " + permName);
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
            Context ctx = HiddenApiHelper.getContext();
            PackageManagerHidden pm = Refine.unsafeCast(PackageManagerUtil.getPackageManager(ctx).packageManager());
            int userId = Integer.parseInt(args[1]);
            UserHandle user = UserHandleHidden.of(userId);
            for (PackagePermissionSet permissionSet : parsePackagePermissionSets(args, 2)) {
                for (String permName : permissionSet.permissionNames) {
                    try {
                        pm.revokeRuntimePermission(permissionSet.packageName, permName, user);
                    } catch (Exception e) {
                        System.out.println("Failed, skip: " + permissionSet.packageName + " " + permName);
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
                            System.out.println("Failed, skip: " + opModeSet.packageName + " " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed, skip: " + opModeSet.packageName + " " + e.getMessage());
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
                    throw new IllegalArgumentException("Missing mode for op: " + tokens.get(j));
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
                        throw new IllegalArgumentException("Nested group: " + rawToken);
                    }
                    current = new ArrayList<>();
                    rawToken = rawToken.substring(1);
                }
                if (current == null) {
                    throw new IllegalArgumentException("Missing group start: " + rawToken);
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
            throw new IllegalArgumentException("Missing group end");
        }
        return groups;
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
