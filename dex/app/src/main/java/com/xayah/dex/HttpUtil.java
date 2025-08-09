package com.xayah.dex;

import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;

public class HttpUtil {

    private static void onHelp() {
        System.out.println("HttpUtil commands:");
        System.out.println("  help");
        System.out.println();
        System.out.println("  get URL");
    }

    private static void onCommand(String cmd, String[] args) {
        switch (cmd) {
            case "get":
                get(args);
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

    private static void get(String[] args) {
        try {
            String url = args[1];
            Content content = Request.get(url).execute().returnContent();
            System.out.write(content.asBytes());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
