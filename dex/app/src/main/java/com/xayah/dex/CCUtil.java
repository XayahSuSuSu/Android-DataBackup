package com.xayah.dex;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CCUtil {

    private static void onHelp() {
        System.out.println("CCUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  s2t TEXT");
        System.out.println();
        System.out.println("  t2s TEXT");
    }

    private static void onCommand(String cmd, String[] args) {
        switch (cmd) {
            case "s2t":
                s2t(args);
                break;
            case "t2s":
                t2s(args);
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

    /**
     * Simplified Chinese to Traditional Chinese (Taiwan standard)
     */
    private static void s2t(String[] args) {
        if (args.length > 1) {
            try {
                String text = args[1];
                CCHelper ccHelper = new CCHelper();
                System.out.println(ccHelper.s2t(text));
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.exit(1);
            }
        } else {
            CCHelper ccHelper = new CCHelper();
            try {
                if (System.in.available() > 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(ccHelper.s2t(line));
                        }
                        System.exit(0);
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        System.exit(1);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Traditional Chinese to Simplified Chinese
     */
    private static void t2s(String[] args) {
        if (args.length > 1) {
            try {
                String text = args[1];
                CCHelper ccHelper = new CCHelper();
                System.out.println(ccHelper.t2s(text));
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.exit(1);
            }
        } else {
            CCHelper ccHelper = new CCHelper();
            try {
                if (System.in.available() > 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(ccHelper.t2s(line));
                        }
                        System.exit(0);
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                        System.exit(1);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
