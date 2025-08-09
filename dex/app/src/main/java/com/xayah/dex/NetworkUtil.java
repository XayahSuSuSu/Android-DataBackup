package com.xayah.dex;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfigurationHidden;
import android.net.wifi.WifiManagerHidden;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.rikka.tools.refine.Refine;

public class NetworkUtil {
    private static final String NETWORK_PREFIX = "network";
    private static final String NETWORK_SPLIT_SYMBOL = "_";

    private static void onHelp() {
        System.out.println("NetworkUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  getNetworks");
        System.out.println();
        System.out.println("  saveNetworks PATH");
        System.out.println();
        System.out.println("  restoreNetworks PATH");
    }

    private static void onCommand(String cmd, String[] args) {
        switch (cmd) {
            case "getNetworks":
                getNetworks(args);
                break;
            case "saveNetworks":
                saveNetworks(args);
                break;
            case "restoreNetworks":
                restoreNetworks(args);
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

    private static void getNetworks(String[] args) {
        try {
            Context ctx = HiddenApiHelper.getContext();
            WifiManagerHidden wifiManager = Refine.unsafeCast(ctx.getSystemService(Context.WIFI_SERVICE));
            List<WifiConfiguration> networks = wifiManager.getPrivilegedConfiguredNetworks();
            Set<Integer> networkIds = new HashSet<>();
            for (int i = 0; i < networks.size(); i++) {
                WifiConfiguration network = networks.get(i);
                int networkId = network.networkId;
                if (!networkIds.contains(networkId)) {
                    String ssid = network.SSID;
                    String preSharedKey = network.preSharedKey;
                    StringBuilder out = new StringBuilder();
                    out.append(networkId).append(" ").append(ssid);
                    if (preSharedKey != null) {
                        out.append(" ").append(preSharedKey);
                    }
                    System.out.println(out);
                    networkIds.add(networkId);
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void saveNetworks(String[] args) {
        try {
            int status = 0;
            String savePath = args[1];
            Context ctx = HiddenApiHelper.getContext();
            WifiManagerHidden wifiManager = Refine.unsafeCast(ctx.getSystemService(Context.WIFI_SERVICE));
            List<WifiConfiguration> networks = wifiManager.getPrivilegedConfiguredNetworks();
            Set<Integer> networkIds = new HashSet<>();

            File savePathDir = new File(savePath);
            if (!savePathDir.exists()) {
                savePathDir.mkdirs();
            }

            for (int i = 0; i < networks.size(); i++) {
                WifiConfiguration network = networks.get(i);
                int networkId = network.networkId;
                String fileName;
                if (!networkIds.contains(networkId)) {
                    fileName = NETWORK_PREFIX + NETWORK_SPLIT_SYMBOL + network.networkId + NETWORK_SPLIT_SYMBOL + "a";
                    networkIds.add(networkId);
                    System.out.println(network.SSID + " saved");
                } else {
                    fileName = NETWORK_PREFIX + NETWORK_SPLIT_SYMBOL + network.networkId + NETWORK_SPLIT_SYMBOL + "b";
                }
                byte[] config = ParcelableHelper.marshall(network);
                File configFile = new File(savePath, fileName);
                configFile.delete();
                configFile.createNewFile();
                Files.write(configFile.toPath(), config);
                wifiManager.addNetwork(network);
            }
            System.exit(status);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    private static void restoreNetworks(String[] args) {
        try {
            int status = 0;
            String savePath = args[1];
            Context ctx = HiddenApiHelper.getContext();
            WifiManagerHidden wifiManager = Refine.unsafeCast(ctx.getSystemService(Context.WIFI_SERVICE));
            Set<Integer> networkIds = new HashSet<>();

            File savePathDir = new File(savePath);
            if (!savePathDir.exists()) {
                System.out.println(savePath + " is not exists!");
                System.exit(1);
            }

            File[] networkFiles = savePathDir.listFiles();
            if (networkFiles != null) {
                for (File networkFile : networkFiles) {
                    String fileName = networkFile.getName();
                    String[] fileNameArgs = fileName.split(NETWORK_SPLIT_SYMBOL);
                    if (fileNameArgs.length == 3 && fileNameArgs[0].equals(NETWORK_PREFIX)) {
                        int networkId = Integer.parseInt(fileNameArgs[1]);
                        ParcelableHelper.unmarshall(Files.readAllBytes(networkFile.toPath()), parcel -> {
                            WifiConfiguration network = WifiConfigurationHidden.CREATOR.createFromParcel(parcel);
                            wifiManager.addNetwork(network);
                            if (!networkIds.contains(networkId)) {
                                networkIds.add(networkId);
                                System.out.println(network.SSID + " restored");
                            }
                        });
                    }
                }
            }

            System.exit(status);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
