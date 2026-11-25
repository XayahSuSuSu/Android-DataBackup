package top.weixiansen574.hybridfilexfer.core.callback;

public interface ClientCallBack extends TransferFileCallback {
    void onReceiving();
    void onSending();
    void onExit();
}
