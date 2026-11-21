package top.weixiansen574.hybridfilexfer;

import top.weixiansen574.hybridfilexfer.core.HFXService;
import top.weixiansen574.hybridfilexfer.core.Utils;
import top.weixiansen574.hybridfilexfer.core.bean.TrafficInfo;
import top.weixiansen574.hybridfilexfer.core.callback.ClientCallBack;
import top.weixiansen574.hybridfilexfer.core.callback.ConnectServerCallback;
import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback;
import top.weixiansen574.hybridfilexfer.jdkcore.JdkHFXClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final ConnectServerCallback connectServerCallback = new ConnectServerCallback() {
        @Override
        public void onConnectingControlChannel(String address, int port) {
            System.out.println("[HFX-PC] Connecting control channel: " + address + ":" + port);
        }

        @Override
        public void onVersionMismatch(int localVersion, int remoteVersion) {
            System.err.println("[HFX-PC] Protocol version mismatch! PC=" + localVersion + ", Phone=" + remoteVersion);
        }

        @Override
        public void onConnectControlFailed() {
            // 常驻模式下静默重试，不刷屏报错
        }

        @Override
        public void onConnectingTransferChannel(String name, InetAddress inetAddress, InetAddress bindAddress) {
            System.out.println("[HFX-PC] Connecting channel [" + name + "] -> " + inetAddress.getHostAddress()
                    + (bindAddress != null ? " (bind " + bindAddress.getHostAddress() + ")" : ""));
        }

        @Override
        public void onConnectTransferChannelFailed(String name, InetAddress inetAddress, Exception e) {
            System.err.println("[HFX-PC] Channel [" + name + "] failed: " + e.getMessage());
        }

        @Override
        public void onOOM(int createdBuffers, int requiredBuffers, long maxMemoryMB, String osArch) {
            System.err.println("[HFX-PC] OOM allocating buffers: " + createdBuffers + "/" + requiredBuffers
                    + " (maxMem=" + maxMemoryMB + "MB, arch=" + osArch + ")");
        }

        @Override
        public void onRemoteOOM() {
            System.err.println("[HFX-PC] Phone reported OOM while allocating buffers.");
        }

        @Override
        public void onConnectSuccess(List<String> channelNames) {
            System.out.println("[HFX-PC] Connected! Active channels: " + channelNames);
        }
    };

    private static final ClientCallBack clientCallBack = new ClientCallBack() {
        @Override
        public void onReceiving() {
            System.out.println("[HFX-PC] Receiving batch...");
        }

        @Override
        public void onSending() {
            System.out.println("[HFX-PC] Sending batch...");
        }

        @Override
        public void onExit() {
            System.out.println("[HFX-PC] Session detached/closed.");
        }

        @Override
        public void onFileUploading(String iName, String path, long targetSize, long totalSize) {
        }

        @Override
        public void onFileDownloading(String iName, String path, long targetSize, long totalSize) {
        }

        @Override
        public void onSpeedInfo(List<TrafficInfo> trafficInfoList) {
            StringBuilder sb = new StringBuilder("[HFX-PC] Speed:");
            long total = 0;
            for (TrafficInfo info : trafficInfoList) {
                long speed = info.downloadTraffic + info.uploadTraffic;
                total += speed;
                sb.append(" ").append(info.iName).append("=").append(Utils.formatSpeed(speed));
            }
            if (total > 0) {
                sb.append(" | Total=").append(Utils.formatSpeed(total));
                System.out.println(sb);
            }
        }

        @Override
        public void onChannelComplete(String iName, long traffic, long time) {
            long safeTime = Math.max(1L, time);
            System.out.printf("[HFX-PC] Channel [%s] done: %.2f MB (%s)%n",
                    iName, traffic / (1024.0 * 1024.0), Utils.formatSpeed(traffic * 1000L / safeTime));
        }

        @Override
        public void onChannelError(String iName, int errorType, String message) {
            System.err.println("[HFX-PC] Channel [" + iName + "] error type=" + errorType
                    + (message != null ? " msg=" + message : ""));
        }

        @Override
        public void onReadFileError(String message) {
            System.err.println("[HFX-PC] Read file error: " + message);
        }

        @Override
        public void onWriteFileError(String message) {
            System.err.println("[HFX-PC] Write file error: " + message);
        }

        @Override
        public void onComplete(boolean isUpload, long traffic, long time) {
            long safeTime = Math.max(1L, time);
            System.out.printf("[HFX-PC] Batch complete (%s): %.2f MB in %dms (%s)%n",
                    isUpload ? "Upload" : "Download", traffic / (1024.0 * 1024.0), safeTime,
                    Utils.formatSpeed(traffic * 1000L / safeTime));
        }

        @Override
        public void onIncomplete() {
            System.err.println("[HFX-PC] Batch incomplete (connection interrupted).");
        }
    };

    public static void main(String[] args) {
        Map<String, String> argMap = parseArguments(args);
        if (argMap.containsKey("h") || argMap.containsKey("help")) {
            printHelp();
            return;
        }
        if (argMap.containsKey("v") || argMap.containsKey("version")) {
            System.out.println("High-Speed DataBackup PC Receiver (Protocol V" + HFXService.VERSION_CODE + ")");
            return;
        }

        String connectArg = argMap.getOrDefault("c", argMap.getOrDefault("connect", "adb"));
        int port = Integer.parseInt(argMap.getOrDefault("p", argMap.getOrDefault("port", "5740")));
        String dirArg = argMap.getOrDefault("d", argMap.getOrDefault("dir", ""));
        String deviceSerial = argMap.getOrDefault("s", argMap.getOrDefault("device", argMap.get("switch-device")));
        boolean onceMode = argMap.containsKey("once");
        boolean askDir = argMap.containsKey("ask-dir");
        boolean askIp = argMap.containsKey("ask-ip");

        if (askIp && ("prompt".equalsIgnoreCase(connectArg) || connectArg.trim().isEmpty())) {
            try {
                System.out.println("====================================================");
                System.out.println(" High-Speed DataBackup PC Receiver (Wi-Fi Mode)");
                System.out.println("====================================================");
                String line = readPromptLine("Enter phone Wi-Fi IP (e.g. 192.168.31.63): ");
                if (line != null && !line.trim().isEmpty()) {
                    connectArg = line.trim();
                } else {
                    System.err.println("No IP entered, exiting.");
                    return;
                }
            } catch (IOException e) {
                System.err.println("Failed to read IP: " + e.getMessage());
                return;
            }
        }

        if (askDir && (dirArg == null || dirArg.trim().isEmpty() || "true".equalsIgnoreCase(dirArg))) {
            String defaultDir = Paths.get("BackupData").toAbsolutePath().normalize().toString();
            try {
                System.out.println("====================================================");
                System.out.println(" High-Speed DataBackup PC Receiver (Protocol V" + HFXService.VERSION_CODE + ")");
                System.out.println(" Drag a backup folder here & press Enter, or press Enter for default:");
                System.out.println(" Default: " + defaultDir);
                System.out.println("====================================================");
                String line = readPromptLine("Backup Root Path: ");
                if (line != null) {
                    line = line.trim();
                    if (line.startsWith("\"") && line.endsWith("\"") && line.length() >= 2) {
                        line = line.substring(1, line.length() - 1).trim();
                    }
                    dirArg = line.isEmpty() ? defaultDir : line;
                } else {
                    dirArg = defaultDir;
                }
            } catch (IOException e) {
                dirArg = defaultDir;
            }
        }
        if (dirArg == null || dirArg.trim().isEmpty() || "true".equalsIgnoreCase(dirArg)) {
            dirArg = "./BackupData";
        }

        Path rootPath = Paths.get(dirArg).toAbsolutePath().normalize();
        System.out.println("====================================================");
        System.out.println(" High-Speed DataBackup PC Receiver (Protocol V" + HFXService.VERSION_CODE + ")");
        System.out.println(" Mode        : " + (onceMode ? "Once" : "Daemon (Auto-Reconnect)"));
        System.out.println(" Connect     : " + connectArg + " (port " + port + ")");
        System.out.println(" Backup Root : " + rootPath);
        System.out.println(" Remote /DataBackup -> " + rootPath.resolve("DataBackup"));
        System.out.println("====================================================");

        String serverAddr = "adb".equalsIgnoreCase(connectArg) ? "127.0.0.1" : connectArg;
        boolean waitingPrinted = false;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                if ("adb".equalsIgnoreCase(connectArg)) {
                    boolean adbOk = executeAdbForward(port, deviceSerial);
                    if (!adbOk) {
                        if (!waitingPrinted) {
                            System.out.println("[HFX-PC] Waiting for ADB device...");
                            waitingPrinted = true;
                        }
                        if (onceMode)
                            return;
                        Thread.sleep(2000);
                        continue;
                    }
                }

                JdkHFXClient client = new JdkHFXClient(serverAddr, port, rootPath.toString());
                if (client.connect(connectServerCallback)) {
                    waitingPrinted = false;
                    client.start(clientCallBack);
                } else {
                    if (!waitingPrinted) {
                        System.out.println("[HFX-PC] Waiting for DataBackup on phone (port " + port + ")...");
                        waitingPrinted = true;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[HFX-PC] Session ended with error: " + e.getMessage());
            }

            if (onceMode) {
                break;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static String readPromptLine(String prompt) throws IOException {
        java.io.Console console = System.console();
        if (console != null) {
            return console.readLine("%s", prompt);
        }
        System.out.print(prompt);
        System.out.flush();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        return stdin.readLine();
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar pc-receiver.jar [options]");
        System.out.println("  -c, --connect <adb|IP>   Connection mode: 'adb' (default) or phone IP");
        System.out.println("  -p, --port <port>        Server port (default: 5740)");
        System.out.println("  -d, --dir <path>         PC backup root directory (default: ./BackupData)");
        System.out.println("  -s, --device <serial>    ADB device serial (optional)");
        System.out.println("  --once                   Exit after one session instead of auto-reconnect daemon");
        System.out.println("  -v, --version            Show protocol version");
        System.out.println("  -h, --help               Show this help");
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String body = arg.substring(2);
                int eq = body.indexOf('=');
                if (eq > 0) {
                    map.put(body.substring(0, eq), body.substring(eq + 1));
                } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    map.put(body, args[++i]);
                } else {
                    map.put(body, "true");
                }
            } else if (arg.startsWith("-") && arg.length() > 1) {
                String key = arg.substring(1);
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    map.put(key, args[++i]);
                } else {
                    map.put(key, "true");
                }
            }
        }
        return map;
    }

    private static String resolveAdbPath() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String exe = isWindows ? "adb.exe" : "adb";
        File[] candidates = new File[] {
                new File("tools", exe),
                new File(exe)
        };
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                return candidate.getAbsolutePath();
            }
        }
        return "adb";
    }

    private static boolean executeAdbForward(int port, String deviceSerial) {
        String adb = resolveAdbPath();
        ProcessBuilder pb;
        if (deviceSerial != null && !deviceSerial.isEmpty()) {
            pb = new ProcessBuilder(adb, "-s", deviceSerial, "forward", "tcp:" + port, "tcp:" + port);
        } else {
            pb = new ProcessBuilder(adb, "forward", "tcp:" + port, "tcp:" + port);
        }
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (br.readLine() != null) {
                    // drain output
                }
            }
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
