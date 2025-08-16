package com.xayah.dex;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManagerHidden;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.rikka.tools.refine.Refine;

public class NetworkUtil {
    private static final String NETWORK_PREFIX = "network";
    private static final String NETWORK_SPLIT_SYMBOL = "_";
    private static final String[] SKIP_FIELDS = {"mNetworkSeclectionDisableCounter"};

    private static void onHelp() {
        System.out.println("NetworkUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  getNetworks");
        System.out.println("    Dump networks.");
        System.out.println();
        System.out.println("  saveNetworks");
        System.out.println("    Print all networks as JSON to standard output.");
        System.out.println();
        System.out.println("  restoreNetworks FILE");
        System.out.println("    Restore all networks from a JSON file.");
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
            Context ctx = HiddenApiHelper.getContext();
            WifiManagerHidden wifiManager = Refine.unsafeCast(ctx.getSystemService(Context.WIFI_SERVICE));
            List<WifiConfiguration> networks = wifiManager.getPrivilegedConfiguredNetworks();
            Gson gson = new Gson();
            System.out.println(gson.toJson(networks));
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }

    public static class NetworkStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return Arrays.asList(SKIP_FIELDS).contains(f.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    private static void restoreNetworks(String[] args) {
        try {
            int status = 0;
            String jsonPath = args[1];
            Context ctx = HiddenApiHelper.getContext();
            WifiManagerHidden wifiManager = Refine.unsafeCast(ctx.getSystemService(Context.WIFI_SERVICE));
            Set<Integer> networkIds = new HashSet<>();
            Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(new NetworkStrategy()).create();
            File jsonFile = new File(jsonPath);
            if (!jsonFile.exists()) {
                System.out.println(jsonPath + " not exists!");
                System.exit(1);
            }
            try {
                String json = new String(Files.readAllBytes(jsonFile.toPath()));
                WifiConfiguration[] networks = gson.fromJson(json, WifiConfiguration[].class);
                for (WifiConfiguration network : networks) {
                    try {
                        int networkId = network.networkId;
                        network.networkId = -1;
                        wifiManager.addNetwork(network);
                        if (!networkIds.contains(networkId)) {
                            networkIds.add(networkId);
                            System.out.println(network.SSID + " restored");
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        status = 1;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                status = 1;
            }
            System.exit(status);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
