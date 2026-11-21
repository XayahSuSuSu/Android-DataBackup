package top.weixiansen574.hybridfilexfer.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import top.weixiansen574.hybridfilexfer.core.bean.Directory;
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;
import top.weixiansen574.hybridfilexfer.core.callback.TransferFileCallback;
import top.weixiansen574.nio.DataByteChannel;

public abstract class HFXService {
    public static final String CLIENT_HEADER = "HFXC";
    public static final int VERSION_CODE = 401;
    protected final LinkedBlockingDeque<ByteBuffer> buffers = new LinkedBlockingDeque<>();
    protected DataByteChannel ctChannel;
    protected List<TransferConnection> connections;

    protected boolean sendFiles(List<RemoteFile> fileList, Directory localDir, Directory remoteDir, TransferFileCallback callback) throws IOException {
        ReadFileCall readFileCall = createReadFileCall(buffers, fileList, localDir, remoteDir, connections.size());
        return sendWithReadCall(readFileCall, callback);
    }

    protected boolean sendWithReadCall(ReadFileCall readFileCall, TransferFileCallback callback) throws IOException {
        FutureTask<Void> readFileTask = new FutureTask<>(readFileCall);
        Thread readThread = new Thread(readFileTask);
        readThread.setName("FileRead");
        readThread.start();

        SpeedMonitorThread speedMonitorThread = new SpeedMonitorThread(connections, callback);
        speedMonitorThread.setName("SpeedMonitor");
        speedMonitorThread.start();

        long startTime = System.currentTimeMillis();
        List<FutureTask<Void>> transferTasks = new ArrayList<>(connections.size());
        for (TransferConnection connection : connections) {
            FutureTask<Void> task = new FutureTask<>(new SendFileCall(readFileCall, connection, callback));
            transferTasks.add(task);
            Thread thread = new Thread(task);
            thread.setName("UL_" + connection.iName);
            thread.start();
        }

        boolean complete;
        try {
            complete = ctChannel.readBoolean();
        } catch (IOException e) {
            speedMonitorThread.cancel();
            readFileCall.shutdownByConnectionBreak();
            awaitTasksQuietly(transferTasks, readFileTask);
            callback.onIncomplete();
            return false;
        }
        speedMonitorThread.cancel();

        if (!complete) {
            String errMsg = ctChannel.readUTF();
            callback.onWriteFileError(errMsg);
            readFileCall.shutdownByWriteError();
            awaitTasksQuietly(transferTasks, readFileTask);
            return true;
        }

        for (FutureTask<Void> transferTask : transferTasks) {
            try {
                transferTask.get();
            } catch (ExecutionException | InterruptedException e) {
                readFileCall.shutdownByConnectionBreak();
                awaitTasksQuietly(transferTasks, readFileTask);
                callback.onIncomplete();
                return false;
            }
        }

        long totalUploadTraffic = 0;
        for (TransferConnection connection : connections) {
            totalUploadTraffic += connection.resetTotalTrafficInfo().uploadTraffic;
        }

        try {
            readFileTask.get();
            ctChannel.writeBoolean(true);
        } catch (ExecutionException | InterruptedException e) {
            Throwable cause = e.getCause();
            String ex = cause != null ? cause.toString() : e.toString();
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(ex);
            callback.onReadFileError(ex);
            return true;
        }

        callback.onComplete(true, totalUploadTraffic, System.currentTimeMillis() - startTime);
        return true;
    }

    protected boolean receiveFiles(TransferFileCallback callback) throws IOException {
        WriteFileCall writeFileCall = createWriteFileCall(buffers, connections.size());
        return receiveWithWriteCall(writeFileCall, callback, null, null);
    }

    protected boolean receiveWithWriteCall(
            WriteFileCall writeFileCall,
            TransferFileCallback callback,
            ThrowingRunnable onBeforeAckSuccess,
            Runnable onErrorCleanup
    ) throws IOException {
        long startTime = System.currentTimeMillis();

        SpeedMonitorThread speedMonitorThread = new SpeedMonitorThread(connections, callback);
        speedMonitorThread.setName("SpeedMonitor");
        speedMonitorThread.start();

        List<FutureTask<Void>> transferTasks = new ArrayList<>(connections.size());
        for (int i = 0; i < connections.size(); i++) {
            TransferConnection connection = connections.get(i);
            FutureTask<Void> task = new FutureTask<>(new ReceiveFileCall(i, connection, writeFileCall, callback));
            transferTasks.add(task);
            Thread thread = new Thread(task);
            thread.setName("DL_" + connection.iName);
            thread.start();
        }
        FutureTask<Void> writeFileTask = new FutureTask<>(writeFileCall);
        Thread thread = new Thread(writeFileTask);
        thread.setName("FileWrite");
        thread.start();

        try {
            writeFileTask.get();
        } catch (InterruptedException | ExecutionException e) {
            speedMonitorThread.cancel();
            writeFileCall.cancel();
            if (onErrorCleanup != null) onErrorCleanup.run();
            Throwable cause = e.getCause();
            String ex = cause != null ? cause.toString() : e.toString();
            ctChannel.writeBoolean(false);
            ctChannel.writeUTF(ex);
            awaitTasksQuietly(transferTasks, null);
            callback.onWriteFileError(ex);
            return true;
        }

        for (FutureTask<Void> task : transferTasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                speedMonitorThread.cancel();
                writeFileCall.cancel();
                if (onErrorCleanup != null) onErrorCleanup.run();
                try {
                    ctChannel.writeBoolean(true);
                } catch (IOException ignored) {
                }
                callback.onIncomplete();
                return false;
            }
        }
        speedMonitorThread.cancel();

        if (onBeforeAckSuccess != null) {
            try {
                onBeforeAckSuccess.run();
            } catch (Exception e) {
                if (onErrorCleanup != null) onErrorCleanup.run();
                ctChannel.writeBoolean(false);
                ctChannel.writeUTF(e.toString());
                callback.onWriteFileError(e.toString());
                return true;
            }
        }

        ctChannel.writeBoolean(true);
        if (ctChannel.readBoolean()) {
            long totalDownloadTraffic = 0;
            for (TransferConnection connection : connections) {
                totalDownloadTraffic += connection.resetTotalTrafficInfo().downloadTraffic;
            }
            callback.onComplete(false, totalDownloadTraffic, System.currentTimeMillis() - startTime);
        } else {
            if (onErrorCleanup != null) onErrorCleanup.run();
            callback.onReadFileError(ctChannel.readUTF());
        }
        return true;
    }

    private void awaitTasksQuietly(List<FutureTask<Void>> transferTasks, FutureTask<Void> ioTask) {
        if (ioTask != null) {
            try {
                ioTask.get(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                ioTask.cancel(true);
            }
        }
        for (FutureTask<Void> t : transferTasks) {
            try {
                t.get(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                t.cancel(true);
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    protected abstract WriteFileCall createWriteFileCall(LinkedBlockingDeque<ByteBuffer> buffers, int dequeCount);

    protected abstract ReadFileCall createReadFileCall(LinkedBlockingDeque<ByteBuffer> buffers, List<RemoteFile> files, Directory localDir, Directory remoteDir, int operateThreadCount);
}
