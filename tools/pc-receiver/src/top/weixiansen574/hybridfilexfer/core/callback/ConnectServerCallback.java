package top.weixiansen574.hybridfilexfer.core.callback;

import java.net.InetAddress;
import java.util.List;

public interface ConnectServerCallback {
    void onConnectingControlChannel(String address, int port);

    void onVersionMismatch(int localVersion, int remoteVersion);

    void onConnectControlFailed();

    void onConnectingTransferChannel(
            String name,
            InetAddress inetAddress,
            InetAddress bindAddress);

    void onConnectTransferChannelFailed(String name, InetAddress inetAddress, Exception e);

    void onOOM(
            int createdBuffers,
            int requiredBuffers,
            long maxMemoryMB,
            String osArch);

    void onRemoteOOM();

    void onConnectSuccess(List<String> channelNames);
}
