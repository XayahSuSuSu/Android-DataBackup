package top.weixiansen574.hybridfilexfer.jdkcore;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import top.weixiansen574.hybridfilexfer.core.ReadFileCall;
import top.weixiansen574.hybridfilexfer.core.bean.Directory;
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;

public class JdkReadFileCall extends ReadFileCall {
    private final VirtualPathResolver resolver;
    private RandomAccessFile currentRaf;
    private FileChannel currentChannel;

    public JdkReadFileCall(
            LinkedBlockingDeque<ByteBuffer> buffers,
            List<RemoteFile> files,
            Directory localDir,
            Directory remoteDir,
            int operateThreadCount,
            VirtualPathResolver resolver
    ) {
        super(buffers, files, localDir, remoteDir, operateThreadCount);
        this.resolver = resolver;
    }

    @Override
    protected boolean fileExists(String path) throws Exception {
        return Files.exists(resolver.resolve(path));
    }

    @Override
    protected List<RemoteFile> listFiles(String path) throws Exception {
        Path real = resolver.resolve(path);
        File[] files = real.toFile().listFiles();
        if (files == null) {
            return null;
        }
        List<RemoteFile> remoteFiles = new ArrayList<>(files.length);
        for (File file : files) {
            String vPath = resolver.toVirtualPath(file.toPath());
            remoteFiles.add(new RemoteFile(file.getName(), vPath, file.lastModified(), file.isDirectory() ? 0L : file.length(), file.isDirectory()));
        }
        return remoteFiles;
    }

    @Override
    protected FileChannel openFile(String path) throws Exception {
        Path real = resolver.resolve(path);
        currentRaf = new RandomAccessFile(real.toFile(), "r");
        currentChannel = currentRaf.getChannel();
        return currentChannel;
    }

    @Override
    protected void closeFile() throws Exception {
        if (currentChannel != null) {
            currentChannel.close();
            currentChannel = null;
        }
        if (currentRaf != null) {
            currentRaf.close();
            currentRaf = null;
        }
    }
}
